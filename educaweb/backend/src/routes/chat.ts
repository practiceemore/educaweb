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
    let respostaOriginal: string = ''; // Preservar resposta original para debug
    try {
      const result = await model.generateContent(context);
      const response = await result.response;
      resposta = response.text();
      
      // PRESERVAR resposta original antes de qualquer modificação
      respostaOriginal = resposta;
      
      // LOG: Resposta completa da IA
      console.log('========================================');
      console.log('[GRADE DEBUG] Resposta completa da IA:');
      console.log('========================================');
      console.log(resposta);
      console.log('========================================');
      console.log('[GRADE DEBUG] Resposta original preservada para debug');
      
    } catch (error) {
      console.error('Erro ao gerar resposta com Gemini:', error);
      resposta = `Desculpe, ocorreu um erro ao processar sua mensagem. Tente novamente em alguns instantes.`;
      respostaOriginal = resposta; // Preservar mesmo em caso de erro
    }

    // Verificar se a resposta contém dados de grade horária
    let gradeData = null;
    try {
      console.log('[GRADE DEBUG] Iniciando extração de JSON da resposta...');
      
      // Tentar extrair JSON da resposta
      const jsonMatch = resposta.match(/\{[\s\S]*"action":\s*"generate_grade"[\s\S]*\}/);
      
      if (jsonMatch) {
        const jsonStr = jsonMatch[0];
        console.log('[GRADE DEBUG] JSON extraído (raw string):');
        console.log(jsonStr);
        console.log('========================================');
        
        const parsed = JSON.parse(jsonStr);
        console.log('[GRADE DEBUG] JSON parseado (objeto):');
        console.log(JSON.stringify(parsed, null, 2));
        console.log('========================================');
        
        if (parsed.action === 'generate_grade' && parsed.data) {
          gradeData = parsed.data;
          console.log('[GRADE DEBUG] gradeData encontrado e válido!');
          console.log(`[GRADE DEBUG] Número de turmas: ${gradeData.turmas?.length || 0}`);
        } else {
          console.log('[GRADE DEBUG] JSON encontrado mas action ou data inválidos');
          console.log(`[GRADE DEBUG] parsed.action: ${parsed.action}`);
          console.log(`[GRADE DEBUG] parsed.data existe: ${!!parsed.data}`);
        }
      } else {
        console.log('[GRADE DEBUG] Nenhum JSON encontrado na resposta com match pattern');
        console.log('[GRADE DEBUG] Tentando buscar por "generate_grade" no texto...');
        const hasGenerateGrade = resposta.includes('generate_grade');
        console.log(`[GRADE DEBUG] Contém "generate_grade": ${hasGenerateGrade}`);
      }
    } catch (error) {
      console.error('[GRADE DEBUG] Erro ao processar JSON de grade horária:', error);
      console.error('[GRADE DEBUG] Stack trace:', error instanceof Error ? error.stack : 'N/A');
      console.log('Resposta não contém dados de grade horária ou JSON inválido');
    }

    // Verificar se a resposta contém dados de relatório
    let reportData = null;
    try {
      // Primeiro, remover marcadores de código markdown (```json e ```)
      let cleanResponse = resposta;
      
      // Remover blocos de código markdown (pode estar em qualquer lugar)
      cleanResponse = cleanResponse.replace(/```json\s*/gi, '');
      cleanResponse = cleanResponse.replace(/```\s*/g, '');
      
      // Também remover se estiver no início/fim após a limpeza acima
      cleanResponse = cleanResponse.trim();
      
      // Tentar encontrar JSON completo - procurar por início e fim do objeto JSON
      let jsonStart = cleanResponse.indexOf('"action":');
      if (jsonStart === -1) {
        jsonStart = cleanResponse.indexOf('"action" :');
      }
      if (jsonStart === -1) {
        jsonStart = cleanResponse.indexOf('generate_report');
      }
      
      if (jsonStart !== -1) {
        // Encontrar o início do objeto { antes do "action"
        let startIdx = cleanResponse.lastIndexOf('{', jsonStart);
        if (startIdx === -1) startIdx = cleanResponse.indexOf('{', jsonStart);
        
        if (startIdx !== -1) {
          // Encontrar o final do objeto JSON - contar chaves
          let braceCount = 0;
          let inString = false;
          let escapeNext = false;
          
          for (let i = startIdx; i < cleanResponse.length; i++) {
            const char = cleanResponse[i];
            
            if (escapeNext) {
              escapeNext = false;
              continue;
            }
            
            if (char === '\\') {
              escapeNext = true;
              continue;
            }
            
            if (char === '"' && !escapeNext) {
              inString = !inString;
              continue;
            }
            
            if (!inString) {
              if (char === '{') braceCount++;
              if (char === '}') {
                braceCount--;
                if (braceCount === 0) {
                  // Encontrou o final do objeto
                  const jsonStr = cleanResponse.substring(startIdx, i + 1);
                  try {
                    const parsed = JSON.parse(jsonStr);
                    if (parsed.action === 'generate_report' && parsed.metadata && parsed.content) {
                      reportData = {
                        metadata: parsed.metadata,
                        content: parsed.content
                      };
                      console.log('Relatório detectado com sucesso!');
                      break;
                    }
                  } catch (parseError) {
                    console.log('Erro ao fazer parse do JSON:', parseError);
                  }
                }
              }
            }
          }
        }
      }
      
      if (!reportData) {
        console.log('Nenhum JSON de relatório válido encontrado na resposta');
      }
    } catch (error) {
      console.log('Erro ao processar dados de relatório:', error);
    }

    // Processar dados da grade horária se encontrados
    let gradeGenerated = false;
    let totalAulasProcessadas = 0;
    let totalAulasCriadas = 0;
    let totalAulasFalhadas = 0;
    const aulasFalhadas: Array<{ motivo: string; dados: any }> = [];
    
    if (gradeData && gradeData.turmas) {
      console.log('[GRADE DEBUG] ========================================');
      console.log('[GRADE DEBUG] Iniciando processamento da grade horária');
      console.log('[GRADE DEBUG] ========================================');
      console.log(`[GRADE DEBUG] Total de turmas a processar: ${gradeData.turmas.length}`);
      
      gradeGenerated = true;
      try {
        // Limpar grade existente para as turmas
        const turmaIds = gradeData.turmas.map((t: any) => t.turmaId);
        console.log(`[GRADE DEBUG] Limpando grade existente para turmas: ${turmaIds.join(', ')}`);
        
        const deleteResult = await prisma.gradeHoraria.deleteMany({
          where: {
            turmaId: { in: turmaIds },
            userId
          }
        });
        
        console.log(`[GRADE DEBUG] Aulas deletadas: ${deleteResult.count}`);

        // Criar novas entradas na grade horária
        for (const turmaData of gradeData.turmas) {
          console.log(`[GRADE DEBUG] ----------------------------------------`);
          console.log(`[GRADE DEBUG] Processando turma: ${turmaData.turmaNome || turmaData.turmaId}`);
          console.log(`[GRADE DEBUG] Turma ID: ${turmaData.turmaId}`);
          console.log(`[GRADE DEBUG] Total de aulas na turma: ${turmaData.grade?.length || 0}`);
          
          if (!turmaData.grade || !Array.isArray(turmaData.grade)) {
            console.log(`[GRADE DEBUG] ERRO: turmaData.grade não é um array válido!`);
            console.log(`[GRADE DEBUG] turmaData.grade:`, turmaData.grade);
            continue;
          }
          
          for (const aula of turmaData.grade) {
            totalAulasProcessadas++;
            
            console.log(`[GRADE DEBUG] --- Processando aula ${totalAulasProcessadas} ---`);
            console.log(`[GRADE DEBUG] Disciplina (nome): ${aula.disciplina}`);
            console.log(`[GRADE DEBUG] Professor (nome): ${aula.professor}`);
            console.log(`[GRADE DEBUG] Sala (nome): ${aula.sala}`);
            console.log(`[GRADE DEBUG] Dia: ${aula.diaSemana}, Horário: ${aula.horarioInicio}-${aula.horarioFim}`);
            
            // Buscar IDs dos objetos por nome
            const disciplina = await prisma.disciplina.findFirst({
              where: { nome: aula.disciplina, userId }
            });
            
            if (!disciplina) {
              console.log(`[GRADE DEBUG] ❌ Disciplina "${aula.disciplina}" NÃO encontrada no banco`);
              totalAulasFalhadas++;
              aulasFalhadas.push({
                motivo: `Disciplina "${aula.disciplina}" não encontrada`,
                dados: aula
              });
              continue;
            }
            console.log(`[GRADE DEBUG] ✅ Disciplina encontrada: ID ${disciplina.id}`);
            
            const professor = await prisma.professor.findFirst({
              where: { nome: aula.professor, userId }
            });
            
            if (!professor) {
              console.log(`[GRADE DEBUG] ❌ Professor "${aula.professor}" NÃO encontrado no banco`);
              totalAulasFalhadas++;
              aulasFalhadas.push({
                motivo: `Professor "${aula.professor}" não encontrado`,
                dados: aula
              });
              continue;
            }
            console.log(`[GRADE DEBUG] ✅ Professor encontrado: ID ${professor.id}`);
            
            const sala = await prisma.sala.findFirst({
              where: { nome: aula.sala, userId }
            });
            
            if (!sala) {
              console.log(`[GRADE DEBUG] ❌ Sala "${aula.sala}" NÃO encontrada no banco`);
              totalAulasFalhadas++;
              aulasFalhadas.push({
                motivo: `Sala "${aula.sala}" não encontrada`,
                dados: aula
              });
              continue;
            }
            console.log(`[GRADE DEBUG] ✅ Sala encontrada: ID ${sala.id}`);

            if (disciplina && professor && sala) {
              try {
                const aulaCriada = await prisma.gradeHoraria.create({
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
                
                totalAulasCriadas++;
                console.log(`[GRADE DEBUG] ✅ Aula criada com sucesso! ID: ${aulaCriada.id}`);
              } catch (createError) {
                totalAulasFalhadas++;
                console.error(`[GRADE DEBUG] ❌ Erro ao criar aula no banco:`, createError);
                console.error(`[GRADE DEBUG] Stack trace:`, createError instanceof Error ? createError.stack : 'N/A');
                aulasFalhadas.push({
                  motivo: `Erro ao criar no banco: ${createError instanceof Error ? createError.message : 'Erro desconhecido'}`,
                  dados: aula
                });
              }
            }
          }
        }
        
        console.log('[GRADE DEBUG] ========================================');
        console.log('[GRADE DEBUG] RESUMO DO PROCESSAMENTO:');
        console.log(`[GRADE DEBUG] Total de aulas processadas: ${totalAulasProcessadas}`);
        console.log(`[GRADE DEBUG] Total de aulas criadas: ${totalAulasCriadas}`);
        console.log(`[GRADE DEBUG] Total de aulas falhadas: ${totalAulasFalhadas}`);
        if (aulasFalhadas.length > 0) {
          console.log('[GRADE DEBUG] Aulas que falharam:');
          aulasFalhadas.forEach((falha, index) => {
            console.log(`[GRADE DEBUG] ${index + 1}. ${falha.motivo}`);
            console.log(`[GRADE DEBUG]    Dados:`, JSON.stringify(falha.dados, null, 2));
          });
        }
        console.log('[GRADE DEBUG] ========================================');

      } catch (error) {
        console.error('[GRADE DEBUG] Erro geral ao processar grade horária:', error);
        console.error('[GRADE DEBUG] Stack trace:', error instanceof Error ? error.stack : 'N/A');
        resposta += '\n\n⚠️ Grade horária gerada, mas houve erro ao salvar no banco de dados.';
      }
    } else {
      console.log('[GRADE DEBUG] gradeData não encontrado ou inválido');
      console.log(`[GRADE DEBUG] gradeData existe: ${!!gradeData}`);
      console.log(`[GRADE DEBUG] gradeData.turmas existe: ${!!(gradeData && gradeData.turmas)}`);
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

    // Incluir dados de debug na resposta
    const responseData = {
      ...response,
      gradeGenerated,
      gradeData: gradeGenerated ? gradeData : undefined,
      reportGenerated: !!reportData,
      reportData: reportData || undefined
    };
    
    if (gradeGenerated) {
      (responseData as any).gradeDebug = {
        totalAulasProcessadas,
        totalAulasCriadas,
        totalAulasFalhadas,
        aulasFalhadas: aulasFalhadas.slice(0, 10) // Limitar a 10 para não sobrecarregar
      };
    }
    
    // Incluir resposta completa da IA para debug (usar versão preservada)
    (responseData as any).iaResponseRaw = respostaOriginal || resposta;
    console.log('[GRADE DEBUG] Resposta original preservada:', respostaOriginal ? 'Sim' : 'Não');
    console.log('[GRADE DEBUG] Tamanho da resposta original:', respostaOriginal?.length || 0);
    
    res.json({
      success: true,
      data: responseData
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
