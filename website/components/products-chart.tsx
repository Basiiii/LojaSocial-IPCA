"use client"

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from "recharts"
import { Package, Apple, Sparkles, SprayCan } from "lucide-react"
import { useEffect, useState } from "react"

interface ProductData {
  name: string
  value: number
  color: string
  icon?: React.ComponentType<{ className?: string; style?: React.CSSProperties }>
}

const iconMap: Record<string, React.ComponentType<{ className?: string; style?: React.CSSProperties }>> = {
  "Alimentar": Apple,
  "Higiene Pessoal": Sparkles,
  "Limpeza": SprayCan,
}

const COLORS = ["#2d9c6c", "#4b8fce", "#d4a053"]

export default function ProductsChart() {
  const [productData, setProductData] = useState<ProductData[]>([
    { name: "Alimentar", value: 0, color: "#2d9c6c" },
    { name: "Higiene Pessoal", value: 0, color: "#4b8fce" },
    { name: "Limpeza", value: 0, color: "#d4a053" },
  ])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function fetchProductStats() {
      try {
        console.log('Fetching product stats from API...')
        const response = await fetch('/api/products/stats')
        console.log('Response status:', response.status)
        
        if (response.ok) {
          const data = await response.json()
          console.log('Received data:', data)
          
          if (data.products && Array.isArray(data.products)) {
            const productsWithIcons = data.products.map((product: ProductData) => ({
              ...product,
              icon: iconMap[product.name] || Package,
            }))
            setProductData(productsWithIcons)
            console.log('Product data updated:', productsWithIcons)
          } else {
            console.error('Invalid data format:', data)
          }
        } else {
          const errorData = await response.json().catch(() => ({}))
          console.error('Failed to fetch product statistics:', response.status, errorData)
          setError(`Erro ao carregar dados: ${response.status}`)
        }
      } catch (error) {
        console.error('Error fetching product statistics:', error)
        setError('Erro ao conectar ao servidor')
      } finally {
        setIsLoading(false)
      }
    }

    fetchProductStats()
  }, [])

  return (
    <section id="produtos" className="py-20">
      <div className="container mx-auto px-4">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <span className="text-sm font-medium text-primary uppercase tracking-wider">Produtos</span>
          <h2 className="text-3xl md:text-4xl font-bold text-foreground mt-4 mb-6 text-balance">
            Distribuição de Produtos Disponíveis
          </h2>
          <p className="text-lg text-muted-foreground leading-relaxed">
            A nossa loja social disponibiliza uma variedade de produtos essenciais organizados em três categorias
            principais para melhor atender às necessidades da comunidade.
          </p>
        </div>

        <div className="grid lg:grid-cols-2 gap-8 items-center">
          <Card className="bg-card border-border">
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-foreground">
                <Package className="w-5 h-5 text-primary" />
                Tipos de Produtos
              </CardTitle>
              <CardDescription>Percentagem de produtos disponíveis por categoria</CardDescription>
            </CardHeader>
            <CardContent>
              {isLoading ? (
                <div className="h-[350px] w-full flex items-center justify-center">
                  <div className="text-muted-foreground">A carregar dados...</div>
                </div>
              ) : error ? (
                <div className="h-[350px] w-full flex items-center justify-center">
                  <div className="text-destructive">
                    <p className="font-semibold">Erro ao carregar dados</p>
                    <p className="text-sm text-muted-foreground mt-2">{error}</p>
                    <p className="text-xs text-muted-foreground mt-4">Verifique a consola do navegador para mais detalhes.</p>
                  </div>
                </div>
              ) : (
                <div className="h-[350px] w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={productData}
                        cx="50%"
                        cy="50%"
                        innerRadius={60}
                        outerRadius={120}
                        paddingAngle={5}
                        dataKey="value"
                        label={({ name, value }) => `${value}%`}
                        labelLine={false}
                      >
                        {productData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={entry.color || COLORS[index % COLORS.length]} stroke="transparent" />
                        ))}
                      </Pie>
                      <Tooltip
                        formatter={(value: number) => [`${value}%`, "Percentagem"]}
                        contentStyle={{
                          backgroundColor: "hsl(var(--card))",
                          border: "1px solid hsl(var(--border))",
                          borderRadius: "8px",
                          color: "hsl(var(--foreground))",
                        }}
                      />
                      <Legend
                        verticalAlign="bottom"
                        height={36}
                        formatter={(value) => <span className="text-foreground">{value}</span>}
                      />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              )}
            </CardContent>
          </Card>

          <div className="space-y-6">
            {productData.map((product, index) => {
              const Icon = product.icon || Package
              return (
                <Card
                  key={index}
                  className="bg-card border-border hover:border-primary/30 transition-all hover:shadow-lg"
                >
                  <CardContent className="p-6">
                    <div className="flex items-start gap-4">
                      <div
                        className="w-14 h-14 rounded-xl flex items-center justify-center shrink-0"
                        style={{ backgroundColor: `${product.color}20` }}
                      >
                        <Icon className="w-7 h-7" style={{ color: product.color }} />
                      </div>
                      <div className="flex-1">
                        <div className="flex items-center justify-between mb-2">
                          <h3 className="text-lg font-semibold text-foreground">{product.name}</h3>
                          <span className="text-2xl font-bold" style={{ color: product.color }}>
                            {product.value}%
                          </span>
                        </div>
                        <div className="w-full bg-secondary rounded-full h-2.5">
                          <div
                            className="h-2.5 rounded-full transition-all duration-500"
                            style={{
                              width: `${product.value}%`,
                              backgroundColor: product.color,
                            }}
                          />
                        </div>
                        <p className="text-sm text-muted-foreground mt-3">
                          {product.name === "Alimentar" &&
                            "Produtos alimentares básicos incluindo conservas, cereais, massas e outros itens essenciais."}
                          {product.name === "Higiene Pessoal" &&
                            "Artigos de higiene como sabonetes, pasta de dentes, champô e produtos de cuidado pessoal."}
                          {product.name === "Limpeza" &&
                            "Produtos de limpeza doméstica incluindo detergentes, desinfetantes e outros artigos."}
                        </p>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              )
            })}
          </div>
        </div>
      </div>
    </section>
  )
}
