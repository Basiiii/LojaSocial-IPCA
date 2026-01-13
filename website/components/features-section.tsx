import { Card, CardContent } from "@/components/ui/card"
import { Smartphone, QrCode, Bell, Calendar, BarChart3, Users } from "lucide-react"

const features = [
  {
    icon: Smartphone,
    title: "Interface Intuitiva",
    description: "Navegação simples e fácil acesso a todas as funcionalidades da loja social.",
  },
  {
    icon: QrCode,
    title: "Registo de produtos por código de barras",
    description: "Sistema de registo de produtos por código de barras para facilitar a entrada de produtos no stock.",
  },
  {
    icon: Bell,
    title: "Notificações",
    description: "Receba alertas sobre pedidos e candidaturas.",
  },
  {
    icon: Calendar,
    title: "Agendamento",
    description: "Marque o seu horário de levantamento de forma conveniente.",
  },
  {
    icon: BarChart3,
    title: "Histórico",
    description: "Consulte o histórico de saídas e entradas de produtos.",
  },
  {
    icon: Users,
    title: "Apoio à Comunidade",
    description: "Acompanhamento personalizado do apoio prestado aos membros da comunidade académica.",
  },
]

export default function FeaturesSection() {
  return (
    <section id="funcionalidades" className="py-20 bg-secondary/30">
      <div className="container mx-auto px-4">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <span className="text-sm font-medium text-primary uppercase tracking-wider">Funcionalidades</span>
          <h2 className="text-3xl md:text-4xl font-bold text-foreground mt-4 mb-6 text-balance">
            Tudo o que Precisa numa Aplicação
          </h2>
          <p className="text-lg text-muted-foreground leading-relaxed">
            A aplicação da Loja Social foi desenvolvida para simplificar o acesso aos serviços de apoio social, mantendo
            a dignidade e privacidade de todos os utilizadores.
          </p>
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature, index) => (
            <Card
              key={index}
              className="bg-card border-border hover:border-primary/30 transition-all hover:shadow-lg group"
            >
              <CardContent className="p-6">
                <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center mb-4 group-hover:bg-primary/20 transition-colors">
                  <feature.icon className="w-6 h-6 text-primary" />
                </div>
                <h3 className="text-lg font-semibold text-foreground mb-2">{feature.title}</h3>
                <p className="text-sm text-muted-foreground leading-relaxed">{feature.description}</p>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </section>
  )
}
