import { Card, CardContent } from "@/components/ui/card"
import { Heart, HandHeart, ShieldCheck, Clock } from "lucide-react"

const values = [
  {
    icon: Heart,
    title: "Solidariedade",
    description: "Apoiamos a comunidade académica através da distribuição de produtos essenciais.",
  },
  {
    icon: HandHeart,
    title: "Dignidade",
    description: "Garantimos um acesso discreto e respeitoso a todos os beneficiários.",
  },
  {
    icon: ShieldCheck,
    title: "Confiança",
    description: "Processo seguro e confidencial para proteger a privacidade de todos.",
  },
  {
    icon: Clock,
    title: "Acessibilidade",
    description: "Disponível de segunda a sexta-feira, das 9h às 17h.",
  },
]

export default function AboutSection() {
  return (
    <section id="sobre" className="py-20 bg-secondary/30">
      <div className="container mx-auto px-4">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <span className="text-sm font-medium text-primary uppercase tracking-wider">Sobre Nós</span>
          <h2 className="text-3xl md:text-4xl font-bold text-foreground mt-4 mb-6 text-balance">
            Apoio Social para a Comunidade Académica
          </h2>
          <p className="text-lg text-muted-foreground leading-relaxed">
            A Loja Social do SAS IPCA é um projeto de apoio social que disponibiliza produtos essenciais aos estudantes
            e colaboradores que necessitam de ajuda. A nossa aplicação torna este processo mais simples, digno e
            acessível.
          </p>
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {values.map((value, index) => (
            <Card key={index} className="bg-card border-border hover:border-primary/30 transition-colors">
              <CardContent className="p-6">
                <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center mb-4">
                  <value.icon className="w-6 h-6 text-primary" />
                </div>
                <h3 className="text-lg font-semibold text-foreground mb-2">{value.title}</h3>
                <p className="text-sm text-muted-foreground leading-relaxed">{value.description}</p>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </section>
  )
}
