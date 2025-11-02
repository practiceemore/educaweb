import { Router, Request, Response } from 'express';
import { prisma } from '../index';
import { ChatMessageRequest, ChatMessageResponse } from '../types';
import { AuthRequest } from '../types';
import { GoogleGenerativeAI } from '@google/generative-ai';

const router = Router();

// Configurar Gemini AI
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY!);
const model = genAI.getGenerativeModel({ model: process.env.GEMINI_MODEL || 'gemini-2.5-pro' });

// Obter dados da escola para contexto da IA
router.get('/school-context', async (req: AuthRequest, res: Response) => {
  try {
    const userId = req.user!.id;

    // Buscar todos os dados da escola
    const [turmas, salas, professores, disciplinas, turmaDisciplinas] = await Promise.all([
      // Turmas
      prisma.turma.findMany({
        where: { userId },
        select: {
          id: true,
          nome: true,
          serie: true,
          turno: true,
          capacidade: true,
          alunosMatriculados: true,
          anoLetivo: true,
        },
        orderBy: { nome: 'asc' }
      }),

      // Salas
      prisma.sala.findMany({
        where: { userId },
        select: {
          id: true,
          nome: true,
          capacidade: true,
          tipo: true,
          recursos: true,
          status: true
        },
        orderBy: { nome: 'asc' }
      }),

      // Professores
      prisma.professor.findMany({
        where: { userId },
        select: {
          id: true,
          nome: true,
          email: true,
          telefone: true,
          especialidade: true
        },
        orderBy: { nome: 'asc' }
      }),

      // Disciplinas
      prisma.disciplina.findMany({
        where: { userId },
        select: {
          id: true,
          nome: true,
          descricao: true,
        },
        orderBy: { nome: 'asc' }
      }),

      // Turma-Disciplinas (carga horária)
      prisma.turmaDisciplina.findMany({
        where: { userId },
        include: {
          turma: {
            select: {
              id: true,
              nome: true,
              serie: true
            }
          },
          disciplina: {
            select: {
              id: true,
              nome: true
            }
          }
        },
        orderBy: [
          { turma: { nome: 'asc' } },
          { disciplina: { nome: 'asc' } }
        ]
      })
    ]);

    // Organizar dados para contexto
    const schoolContext = {
      turmas: turmas.map(t => ({
        ...t,
        disciplinas: turmaDisciplinas
          .filter(td => td.turmaId === t.id)
          .map(td => ({
            nome: td.disciplina.nome,
            aulasPorSemana: td.aulasPorSemana
          }))
      })),
      salas,
      professores,
      disciplinas,
      estatisticas: {
        totalTurmas: turmas.length,
        totalSalas: salas.length,
        totalProfessores: professores.length,
        totalDisciplinas: disciplinas.length,
        turmasAtivas: turmas.length,
        salasDisponiveis: salas.filter(s => s.status === 'disponivel').length
      }
    };

    res.json({
      success: true,
      data: schoolContext
    });
  } catch (error) {
    console.error('Erro ao buscar contexto da escola:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Obter histórico de mensagens
router.get('/messages', async (req: AuthRequest, res: Response) => {
  try {
    const { page = 1, limit = 50 } = req.query;
    const skip = (Number(page) - 1) * Number(limit);

    const messages = await prisma.mensagemChat.findMany({
      where: { userId: req.user!.id },
      skip,
      take: Number(limit),
      orderBy: { timestamp: 'desc' }
    });

    res.json({
      success: true,
      data: messages
    });

  } catch (error) {
    console.error('Erro ao obter mensagens:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Enviar mensagem
router.post('/send', async (req: AuthRequest, res: Response) => {
  try {
    const { mensagem, metadata }: ChatMessageRequest = req.body;

    if (!mensagem) {
      return res.status(400).json({
        success: false,
        error: 'Mensagem é obrigatória'
      });
    }

    // Salvar mensagem do usuário
    const userMessage = await prisma.mensagemChat.create({
      data: {
        userId: req.user!.id,
        mensagem,
        metadata: metadata ? JSON.stringify(metadata) : null
      }
    });

    // Obter contexto do histórico de mensagens recentes
    const recentMessages = await prisma.mensagemChat.findMany({
      where: { userId: req.user!.id },
      take: 10,
      orderBy: { timestamp: 'desc' }
    });

    // Buscar dados da escola para contexto
    const userId = req.user!.id;
    const [turmas, salas, professores, disciplinas, turmaDisciplinas] = await Promise.all([
      prisma.turma.findMany({
        where: { userId },
        select: {
          id: true,
          nome: true,
          serie: true,
          turno: true,
          capacidade: true,
          alunosMatriculados: true,
          anoLetivo: true,
        },
        orderBy: { nome: 'asc' }
      }),
      prisma.sala.findMany({
        where: { userId },
        select: {
          id: true,
          nome: true,
          capacidade: true,
          tipo: true,
          recursos: true,
          status: true
        },
        orderBy: { nome: 'asc' }
      }),
      prisma.professor.findMany({
        where: { userId },
        select: {
          id: true,
          nome: true,
          email: true,
          telefone: true,
          especialidade: true
        },
        orderBy: { nome: 'asc' }
      }),
      prisma.disciplina.findMany({
        where: { userId },
        select: {
          id: true,
          nome: true,
          descricao: true,
        },
        orderBy: { nome: 'asc' }
      }),
      prisma.turmaDisciplina.findMany({
        where: { userId },
        include: {
          turma: {
            select: {
              id: true,
              nome: true,
              serie: true
            }
          },
          disciplina: {
            select: {
              id: true,
              nome: true
            }
          }
        },
        orderBy: [
          { turma: { nome: 'asc' } },
          { disciplina: { nome: 'asc' } }
        ]
      })
    ]);

    // Organizar dados da escola
    const schoolData = {
      turmas: turmas.map(t => ({
        ...t,
        disciplinas: turmaDisciplinas
          .filter(td => td.turmaId === t.id)
          .map(td => ({
            nome: td.disciplina.nome,
            aulasPorSemana: td.aulasPorSemana
          }))
      })),
      salas,
      professores,
      disciplinas,
      estatisticas: {
        totalTurmas: turmas.length,
        totalSalas: salas.length,
        totalProfessores: professores.length,
        totalDisciplinas: disciplinas.length,
        turmasAtivas: turmas.length,
        salasDisponiveis: salas.filter(s => s.status === 'disponivel').length
      }
    };

    // Construir contexto para o Gemini
    let context = `Você é um assistente educacional especializado em gestão escolar. Você está ajudando um usuário do sistema EducaWeb, que é uma plataforma de gestão educacional.

DADOS ATUAIS DA ESCOLA:
- Total de Turmas: ${schoolData.estatisticas.totalTurmas} (${schoolData.estatisticas.turmasAtivas} ativas)
- Total de Salas: ${schoolData.estatisticas.totalSalas} (${schoolData.estatisticas.salasDisponiveis} disponíveis)
- Total de Professores: ${schoolData.estatisticas.totalProfessores}
- Total de Disciplinas: ${schoolData.estatisticas.totalDisciplinas}

TURMAS:
${schoolData.turmas.map(t => 
  `- ${t.nome} (${t.serie}º ano, ${t.turno}): ${t.capacidade} alunos, ${t.alunosMatriculados} matriculados, Ano ${t.anoLetivo}
  Disciplinas: ${t.disciplinas.length > 0 ? t.disciplinas.map(d => `${d.nome} (${d.aulasPorSemana}a/sem)`).join(', ') : 'Nenhuma disciplina configurada'}`
).join('\n')}

SALAS:
${schoolData.salas.map(s => 
  `- ${s.nome} (${s.tipo}): ${s.capacidade} lugares, Status: ${s.status}${s.recursos ? `, Recursos: ${s.recursos}` : ''}`
).join('\n')}

PROFESSORES:
${schoolData.professores.map(p => 
  `- ${p.nome}: ${p.especialidade} (${p.email})`
).join('\n')}

DISCIPLINAS:
${schoolData.disciplinas.map(d => 
  `- ${d.nome}${d.descricao ? `: ${d.descricao}` : ''}`
).join('\n')}

Contexto da conversa recente:
${recentMessages.reverse().map(msg => 
  `Usuário: ${msg.mensagem}\nAssistente: ${msg.resposta || '[Sem resposta]'}`
).join('\n\n')}

Mensagem atual do usuário: ${mensagem}

INSTRUÇÕES:
- Use os dados específicos da escola acima para dar respostas precisas e contextualizadas
- Quando mencionar turmas, salas, professores ou disciplinas, use os nomes e dados reais da escola
- Ofereça sugestões práticas baseadas na estrutura atual da escola
- Se perguntarem sobre estatísticas, use os números reais fornecidos
- Seja específico e ofereça soluções práticas relacionadas à gestão educacional desta escola em particular

FUNCIONALIDADE ESPECIAL - GERAÇÃO DE GRADE HORÁRIA:
Se o usuário solicitar a criação de uma grade horária, você deve:
1. Analisar as disciplinas de cada turma e suas cargas horárias
2. Verificar a disponibilidade de professores e salas
3. Criar uma grade otimizada evitando conflitos
4. Responder com um JSON estruturado no seguinte formato:

{
  "action": "generate_grade",
  "data": {
    "turmas": [
      {
        "turmaId": 1,
        "turmaNome": "1 A",
        "grade": [
          {
            "diaSemana": "segunda",
            "horarioInicio": "07:00",
            "horarioFim": "08:00",
            "disciplina": "Matemática",
            "professor": "Nome do Professor",
            "sala": "Nome da Sala"
          }
        ]
      }
    ],
    "conflitos": []
  }
}

REGRAS PARA GERAÇÃO DE GRADE:
- Cada professor só pode estar em uma turma por horário
- Cada sala só pode ser usada por uma turma por horário
- Respeitar a carga horária semanal de cada disciplina
- Distribuir as aulas ao longo da semana de forma equilibrada
- Preferir aulas duplas consecutivas sempre que possível, respeitando restrições e cargas horárias
- Usar horários padrão: 07:00-08:00, 08:00-09:00, 09:00-10:00, 10:00-11:00, 11:00-12:00, 13:00-14:00, 14:00-15:00, 15:00-16:00, 16:00-17:00
- Dias da semana: segunda, terca, quarta, quinta, sexta

FUNCIONALIDADE ESPECIAL - GERAÇÃO DE RELATÓRIOS:
Se o usuário solicitar a criação de um relatório (usando frases como "faça um relatório", "gere um relatório sobre", "elabore um relatório", "crie um relatório"), você deve:
1. Analisar o contexto da conversa para determinar o tema e título do relatório
2. Decidir se deve usar os dados da escola fornecidos (apenas se relevante ao tema solicitado)
3. Criar um relatório estruturado em formato científico/acadêmico
4. Responder COM UM ÚNICO JSON estruturado no seguinte formato:

{
  "action": "generate_report",
  "metadata": {
    "titulo": "Título do relatório baseado no tema solicitado pelo usuário",
    "subtitulo": "Subtítulo opcional",
    "autor": "EducaWeb Sistema",
    "data": "2025-10-31",
    "usaDadosEscola": true/false
  },
  "content": {
    "resumo": "# Resumo\n\nTexto do resumo em Markdown...",
    "secoes": [
      {
        "titulo": "Introdução",
        "nivel": 1,
        "conteudo": "## Introdução\n\nConteúdo da seção em Markdown. Use formatação Markdown para:\n- Títulos (##, ###)\n- Listas (- ou *)\n- Tabelas\n- **Negrito** e *itálico*\n- Citações"
      },
      {
        "titulo": "Desenvolvimento",
        "nivel": 1,
        "conteudo": "## Desenvolvimento\n\nMais conteúdo em Markdown..."
      },
      {
        "titulo": "Conclusão",
        "nivel": 1,
        "conteudo": "## Conclusão\n\nConclusão do relatório em Markdown..."
      }
    ],
    "referencias": ["Referência 1", "Referência 2"]
  }
}

REGRAS PARA GERAÇÃO DE RELATÓRIOS:
- O título deve ser gerado dinamicamente baseado no tema solicitado pelo usuário na conversa
- Use os dados da escola APENAS se o tema do relatório for relacionado à escola do usuário (ex: "carga horária da minha escola", "análise dos professores da escola")
- Se o tema for geral (ex: "educadores mais importantes do Brasil"), NÃO use dados da escola
- Estrutura típica: Resumo, Introdução, Desenvolvimento/Metodologia, Resultados, Conclusão
- Use Markdown para formatação (títulos, listas, tabelas, ênfase)
- Se usar dados da escola, mencione números e nomes reais dos dados fornecidos
- Seja objetivo, claro e profissional no texto
- Referências são opcionais mas recomendadas para relatórios acadêmicos`;

    // Gerar resposta com Gemini
    let resposta: string;
    try {
      const result = await model.generateContent(context);
      const response = await result.response;
      resposta = response.text();
    } catch (error) {
      console.error('Erro ao gerar resposta com Gemini:', error);
      resposta = `Desculpe, ocorreu um erro ao processar sua mensagem. Tente novamente em alguns instantes.`;
    }

    // Verificar se a resposta contém dados de grade horária
    let gradeData = null;
    try {
      // Tentar extrair JSON da resposta
      const jsonMatch = resposta.match(/\{[\s\S]*"action":\s*"generate_grade"[\s\S]*\}/);
      if (jsonMatch) {
        const jsonStr = jsonMatch[0];
        const parsed = JSON.parse(jsonStr);
        if (parsed.action === 'generate_grade' && parsed.data) {
          gradeData = parsed.data;
        }
      }
    } catch (error) {
      console.log('Resposta não contém dados de grade horária ou JSON inválido');
    }

    // Verificar se a resposta contém dados de relatório
    let reportData = null;
    try {
      // Tentar extrair JSON da resposta
      const jsonMatch = resposta.match(/\{[\s\S]*"action":\s*"generate_report"[\s\S]*\}/);
      if (jsonMatch) {
        const jsonStr = jsonMatch[0];
        const parsed = JSON.parse(jsonStr);
        if (parsed.action === 'generate_report' && parsed.metadata && parsed.content) {
          reportData = {
            metadata: parsed.metadata,
            content: parsed.content
          };
        }
      }
    } catch (error) {
      console.log('Resposta não contém dados de relatório ou JSON inválido');
    }

    // Processar dados da grade horária se encontrados
    let gradeGenerated = false;
    if (gradeData && gradeData.turmas) {
      gradeGenerated = true;
      try {
        // Limpar grade existente para as turmas
        const turmaIds = gradeData.turmas.map((t: any) => t.turmaId);
        await prisma.gradeHoraria.deleteMany({
          where: {
            turmaId: { in: turmaIds },
            userId
          }
        });

        // Criar novas entradas na grade horária
        for (const turmaData of gradeData.turmas) {
          for (const aula of turmaData.grade) {
            // Buscar IDs dos objetos por nome
            const disciplina = await prisma.disciplina.findFirst({
              where: { nome: aula.disciplina, userId }
            });
            const professor = await prisma.professor.findFirst({
              where: { nome: aula.professor, userId }
            });
            const sala = await prisma.sala.findFirst({
              where: { nome: aula.sala, userId }
            });

            if (disciplina && professor && sala) {
              await prisma.gradeHoraria.create({
                data: {
                  turmaId: turmaData.turmaId,
                  disciplinaId: disciplina.id,
                  professorId: professor.id,
                  salaId: sala.id,
                  diaSemana: aula.diaSemana,
                  horarioInicio: aula.horarioInicio,
                  horarioFim: aula.horarioFim,
                  ativa: true,
                  userId
                }
              });
            }
          }
        }

      } catch (error) {
        console.error('Erro ao processar grade horária:', error);
        resposta += '\n\n⚠️ Grade horária gerada, mas houve erro ao salvar no banco de dados.';
      }
    }

    // Atualizar mensagem com resposta
    // Se grade foi gerada, NÃO salvar o JSON, apenas mensagem de confirmação
    // Se relatório foi gerado, salvar o JSON completo para o frontend processar
    const respostaParaSalvar = gradeGenerated 
      ? '✅ Grade horária criada com sucesso! Você pode visualizá-la na aba "Grade Horária".'
      : resposta;

    const messageWithResponse = await prisma.mensagemChat.update({
      where: { id: userMessage.id },
      data: { resposta: respostaParaSalvar }
    });

    const response: ChatMessageResponse = {
      id: messageWithResponse.id,
      mensagem: messageWithResponse.mensagem,
      resposta: messageWithResponse.resposta!,
      timestamp: messageWithResponse.timestamp.toISOString()
    };

    res.json({
      success: true,
      data: {
        ...response,
        gradeGenerated,
        gradeData: gradeGenerated ? gradeData : undefined,
        reportGenerated: !!reportData,
        reportData: reportData || undefined
      }
    });

  } catch (error) {
    console.error('Erro ao enviar mensagem:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

// Limpar histórico de mensagens
router.delete('/clear', async (req: AuthRequest, res: Response) => {
  try {
    await prisma.mensagemChat.deleteMany({
      where: { userId: req.user!.id }
    });

    res.json({
      success: true,
      message: 'Histórico de mensagens limpo com sucesso'
    });

  } catch (error) {
    console.error('Erro ao limpar mensagens:', error);
    res.status(500).json({
      success: false,
      error: 'Erro interno do servidor'
    });
  }
});

export default router;
