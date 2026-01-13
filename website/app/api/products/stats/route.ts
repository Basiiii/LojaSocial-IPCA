import { NextResponse } from 'next/server';
import { initializeApp, getApps, cert } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { readFileSync } from 'fs';
import { join } from 'path';

// Initialize Firebase Admin
function getFirestoreInstance() {
  if (!getApps().length) {
    let serviceAccount = null;
    
    // Try to load service account from environment variable or file
    if (process.env.FIREBASE_SERVICE_ACCOUNT) {
      // If service account is provided as JSON string in environment variable
      try {
        serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
      } catch (error) {
        // If JSON parsing fails, try base64 decoding (for production .env files)
        try {
          const decoded = Buffer.from(process.env.FIREBASE_SERVICE_ACCOUNT, 'base64').toString('utf8');
          serviceAccount = JSON.parse(decoded);
        } catch (base64Error) {
          console.error('FIREBASE_SERVICE_ACCOUNT must be valid JSON or base64-encoded JSON');
          return null;
        }
      }
    } else if (process.env.FIREBASE_SERVICE_ACCOUNT_PATH) {
      // If path to service account file is provided (best for production)
      try {
        const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
        serviceAccount = JSON.parse(readFileSync(serviceAccountPath, 'utf8'));
      } catch (error) {
        console.error('Error reading service account file:', error);
        return null;
      }
    } else {
      // Try default path (for local development)
      try {
        const defaultPath = join(process.cwd(), 'admin-sdk.json');
        serviceAccount = JSON.parse(readFileSync(defaultPath, 'utf8'));
      } catch (error) {
        console.warn('Firebase service account not found. Check environment variables.');
        return null;
      }
    }

    if (serviceAccount) {
      try {
        initializeApp({
          credential: cert(serviceAccount),
        });
        console.log('Firebase Admin initialized successfully');
      } catch (error) {
        console.error('Error initializing Firebase:', error);
        return null;
      }
    } else {
      return null;
    }
  }
  
  try {
    return getFirestore();
  } catch (error) {
    console.error('Error getting Firestore instance:', error);
    return null;
  }
}

// Category mapping: 1=Alimentar, 2=Casa/Limpeza, 3=Higiene Pessoal
const CATEGORY_MAP: Record<number, { name: string; color: string }> = {
  1: { name: 'Alimentar', color: '#2d9c6c' },
  2: { name: 'Limpeza', color: '#d4a053' },
  3: { name: 'Higiene Pessoal', color: '#4b8fce' },
};

export async function GET() {
  try {
    console.log('Fetching product statistics...');
    const db = getFirestoreInstance();
    
    // Check if Firebase is initialized
    if (!db) {
      console.warn('Firebase not configured, returning default values');
      return NextResponse.json({
        products: [
          { name: 'Alimentar', value: 0, color: '#2d9c6c', count: 0 },
          { name: 'Higiene Pessoal', value: 0, color: '#4b8fce', count: 0 },
          { name: 'Limpeza', value: 0, color: '#d4a053', count: 0 },
        ],
        error: 'Firebase not configured',
      });
    }

    console.log('Fetching items from Firestore...');
    
    // Count available items by category (quantity - reservedQuantity)
    const categoryCounts: Record<number, number> = {
      1: 0, // Alimentar
      2: 0, // Limpeza
      3: 0, // Higiene Pessoal
    };

    // Fetch ALL items from the items collection (actual stock, not product definitions)
    const itemsSnapshot = await db.collection('items').get();
    console.log(`Found ${itemsSnapshot.size} total items`);
    
    // Create a map of productId -> category for quick lookup
    const productCategoryMap = new Map<string, number>();
    
    // First, fetch all products to get category mapping
    const productsSnapshot = await db.collection('products').get();
    productsSnapshot.forEach((doc) => {
      const data = doc.data();
      const category = data.category;
      if (category) {
        // Use barcode (doc.id) as the key
        productCategoryMap.set(doc.id, category);
      }
    });
    
    // Now count available items (quantity - reservedQuantity) by category
    itemsSnapshot.forEach((doc) => {
      const data = doc.data();
      const quantity = (data.quantity as number) || 0;
      const reservedQuantity = (data.reservedQuantity as number) || 0;
      const availableQuantity = quantity - reservedQuantity;
      
      if (availableQuantity > 0) {
        // Get category from productId or barcode
        const productId = (data.productId as string) || (data.barcode as string) || '';
        const category = productCategoryMap.get(productId);
        
        if (category && categoryCounts.hasOwnProperty(category)) {
          categoryCounts[category] += availableQuantity;
        }
      }
    });

    // Calculate total and percentages
    const total = Object.values(categoryCounts).reduce((sum, count) => sum + count, 0);

    if (total === 0) {
      // Return default values if no products found
      return NextResponse.json({
        products: [
          { name: 'Alimentar', value: 0, color: '#2d9c6c', count: 0 },
          { name: 'Higiene Pessoal', value: 0, color: '#4b8fce', count: 0 },
          { name: 'Limpeza', value: 0, color: '#d4a053', count: 0 },
        ],
      });
    }

    // Calculate percentages and format data
    const products = Object.entries(categoryCounts)
      .map(([categoryId, count]) => {
        const categoryInfo = CATEGORY_MAP[Number(categoryId)];
        if (!categoryInfo) return null;

        return {
          name: categoryInfo.name,
          value: Math.round((count / total) * 100),
          color: categoryInfo.color,
          count: count,
        };
      })
      .filter((item): item is NonNullable<typeof item> => item !== null)
      .sort((a, b) => b.value - a.value); // Sort by percentage descending

    return NextResponse.json({ products });
  } catch (error) {
    console.error('Error fetching product statistics:', error);
    return NextResponse.json(
      { error: 'Failed to fetch product statistics' },
      { status: 500 }
    );
  }
}
