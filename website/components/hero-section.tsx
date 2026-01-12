"use client"

import { Button } from "@/components/ui/button"
import { ArrowRight, Smartphone, Users, Package } from "lucide-react"
import { useEffect, useState } from "react"

interface ProductCounts {
  alimentar: number
  higiene: number
  limpeza: number
}

export default function HeroSection() {
  const [productCounts, setProductCounts] = useState<ProductCounts>({
    alimentar: 0,
    higiene: 0,
    limpeza: 0,
  })
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    async function fetchProductCounts() {
      try {
        const response = await fetch('/api/products/stats')
        if (response.ok) {
          const data = await response.json()
          if (data.products && Array.isArray(data.products)) {
            const counts: ProductCounts = {
              alimentar: 0,
              higiene: 0,
              limpeza: 0,
            }
            
            data.products.forEach((product: { name: string; count?: number }) => {
              if (product.name === 'Alimentar') {
                counts.alimentar = product.count || 0
              } else if (product.name === 'Higiene Pessoal') {
                counts.higiene = product.count || 0
              } else if (product.name === 'Limpeza') {
                counts.limpeza = product.count || 0
              }
            })
            
            setProductCounts(counts)
          }
        }
      } catch (error) {
        console.error('Error fetching product counts:', error)
      } finally {
        setIsLoading(false)
      }
    }

    fetchProductCounts()
  }, [])

  return (
    <section className="relative overflow-hidden py-20 md:py-32">
      <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-accent/5" />

      <div className="container mx-auto px-4 relative">
        <div className="grid lg:grid-cols-2 gap-12 items-center">
          <div className="space-y-8">
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary/10 text-primary text-sm font-medium">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-primary"></span>
              </span>
              Novo: Aplicação disponível
            </div>

            <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold text-foreground leading-tight text-balance">
              Loja Social <br />
              <span className="text-primary">SAS IPCA</span>
            </h1>

            <p className="text-lg md:text-xl text-muted-foreground max-w-xl leading-relaxed">
              Uma aplicação inovadora que facilita o acesso a produtos essenciais para a comunidade académica. Gestão
              simplificada, solidariedade ao alcance de todos.
            </p>
          </div>

          <div className="relative flex justify-center">
            <div className="relative">
              <div className="absolute -inset-4 bg-gradient-to-r from-primary/20 to-accent/20 rounded-3xl blur-2xl" />
              <div className="relative bg-card rounded-3xl border border-border shadow-2xl p-8 max-w-sm">
                <div className="flex items-center gap-3 mb-6">
                  <div className="w-12 h-12 rounded-xl bg-primary flex items-center justify-center">
                    <Smartphone className="w-6 h-6 text-primary-foreground" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-foreground">App Loja Social</h3>
                    <p className="text-sm text-muted-foreground">SAS IPCA</p>
                  </div>
                </div>

                <div className="space-y-4">
                  <div className="flex items-center gap-3 p-3 rounded-lg bg-secondary">
                    <div className="w-8 h-8 rounded-full bg-chart-1 flex items-center justify-center text-primary-foreground text-xs font-bold">
                      A
                    </div>
                    <div className="flex-1">
                      <p className="text-sm font-medium text-foreground">Produtos Alimentares</p>
                      <p className="text-xs text-muted-foreground">
                        {isLoading ? 'A carregar...' : `${productCounts.alimentar} ${productCounts.alimentar === 1 ? 'item' : 'itens'}`}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3 p-3 rounded-lg bg-secondary">
                    <div className="w-8 h-8 rounded-full bg-chart-2 flex items-center justify-center text-primary-foreground text-xs font-bold">
                      H
                    </div>
                    <div className="flex-1">
                      <p className="text-sm font-medium text-foreground">Higiene Pessoal</p>
                      <p className="text-xs text-muted-foreground">
                        {isLoading ? 'A carregar...' : `${productCounts.higiene} ${productCounts.higiene === 1 ? 'item' : 'itens'}`}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3 p-3 rounded-lg bg-secondary">
                    <div className="w-8 h-8 rounded-full bg-chart-3 flex items-center justify-center text-primary-foreground text-xs font-bold">
                      L
                    </div>
                    <div className="flex-1">
                      <p className="text-sm font-medium text-foreground">Produtos de Limpeza</p>
                      <p className="text-xs text-muted-foreground">
                        {isLoading ? 'A carregar...' : `${productCounts.limpeza} ${productCounts.limpeza === 1 ? 'item' : 'itens'}`}
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
