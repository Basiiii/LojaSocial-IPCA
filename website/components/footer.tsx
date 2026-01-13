import { Heart, Mail, Phone, MapPin } from "lucide-react"

export default function Footer() {
  return (
    <footer id="contacto" className="bg-foreground text-background py-16">
      <div className="container mx-auto px-4">  
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-12">
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <div className="flex items-center justify-center w-10 h-10 rounded-full bg-primary">
                <Heart className="w-5 h-5 text-primary-foreground" />
              </div>
              <div>
                <span className="text-xl font-bold">SAS IPCA</span>
                <span className="block text-xs opacity-70">Loja Social</span>
              </div>
            </div>
            <p className="text-sm opacity-70 leading-relaxed">
              Serviços de Ação Social do IPCA, dedicados ao apoio e bem-estar da comunidade académica.
            </p>
          </div>

          <div>
            <h4 className="font-semibold mb-4">Ligações Rápidas</h4>
            <ul className="space-y-3 text-sm opacity-70">
              <li>
                <a href="#sobre" className="hover:opacity-100 transition-opacity">
                  Sobre Nós
                </a>
              </li>
              <li>
                <a href="#produtos" className="hover:opacity-100 transition-opacity">
                  Produtos
                </a>
              </li>
              <li>
                <a href="#funcionalidades" className="hover:opacity-100 transition-opacity">
                  Funcionalidades
                </a>
              </li>
              <li>
                <a href="#" className="hover:opacity-100 transition-opacity">
                  Política de Privacidade
                </a>
              </li>
            </ul>
          </div>

          <div>
            <h4 className="font-semibold mb-4">Contactos</h4>
            <ul className="space-y-3 text-sm opacity-70">
              <li className="flex items-center gap-2">
                <Mail className="w-4 h-4" />
                <span>sas@ipca.pt</span>
              </li>
              <li className="flex items-center gap-2">
                <Phone className="w-4 h-4" />
                <span>+351 253 802 500</span>
              </li>
              <li className="flex items-start gap-2">
                <MapPin className="w-4 h-4 mt-0.5" />
                <span>Campus do IPCA, Barcelos</span>
              </li>
            </ul>
          </div>
        </div>

        <div className="border-t border-background/10 mt-12 pt-8 flex flex-col md:flex-row justify-between items-center gap-4">
          <p className="text-sm opacity-70 flex items-center gap-1">
            Feito com <Heart className="w-4 h-4 text-primary fill-primary" /> por{" "}
            <span className="font-medium">Enrique Rodrigues</span>,{" "}
            <span className="font-medium">Diogo Machado</span>,{" "}
            <span className="font-medium">José Alves</span> e{" "}
            <span className="font-medium">Carlos Barreiro</span>
          </p>
        </div>
      </div>
    </footer>
  )
}