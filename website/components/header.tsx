"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Menu, X, Heart } from "lucide-react"

export default function Header() {
  const [isMenuOpen, setIsMenuOpen] = useState(false)

  return (
    <header className="sticky top-0 z-50 bg-card/95 backdrop-blur-sm border-b border-border">
      <div className="container mx-auto px-4 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 rounded-full bg-primary">
              <Heart className="w-5 h-5 text-primary-foreground" />
            </div>
            <div>
              <span className="text-xl font-bold text-foreground">SAS IPCA</span>
              <span className="block text-xs text-muted-foreground">Loja Social</span>
            </div>
          </div>

          <nav className="hidden md:flex items-center gap-8">
            <a href="#sobre" className="text-sm font-medium text-muted-foreground hover:text-primary transition-colors">
              Sobre
            </a>
            <a
              href="#produtos"
              className="text-sm font-medium text-muted-foreground hover:text-primary transition-colors"
            >
              Produtos
            </a>
            <a
              href="#funcionalidades"
              className="text-sm font-medium text-muted-foreground hover:text-primary transition-colors"
            >
              Funcionalidades
            </a>
            <a
              href="#contacto"
              className="text-sm font-medium text-muted-foreground hover:text-primary transition-colors"
            >
              Contacto
            </a>
          </nav>


          <button className="md:hidden p-2" onClick={() => setIsMenuOpen(!isMenuOpen)} aria-label="Menu">
            {isMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>

        {isMenuOpen && (
          <nav className="md:hidden pt-4 pb-2 border-t border-border mt-4">
            <div className="flex flex-col gap-4">
              <a href="#sobre" className="text-sm font-medium text-muted-foreground hover:text-primary">
                Sobre
              </a>
              <a href="#produtos" className="text-sm font-medium text-muted-foreground hover:text-primary">
                Produtos
              </a>
              <a href="#funcionalidades" className="text-sm font-medium text-muted-foreground hover:text-primary">
                Funcionalidades
              </a>
              <a href="#contacto" className="text-sm font-medium text-muted-foreground hover:text-primary">
                Contacto
              </a>
              <div className="flex flex-col gap-2 pt-2">
                <Button variant="outline" size="sm" className="w-full bg-transparent">
                  Iniciar Sessão
                </Button>
                <Button size="sm" className="w-full">
                  Descarregar App
                </Button>
              </div>
            </div>
          </nav>
        )}
      </div>
    </header>
  )
}
