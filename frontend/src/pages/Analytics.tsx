import React, { useState, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../api/client';
import type { StudySession, Subject } from '../types';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts';
import {
  TrendingUp,
  Calendar,
  Clock,
  BookOpen,
  Award
} from 'lucide-react';

const MESES_PT = ['Janeiro','Fevereiro','Março','Abril','Maio','Junho','Julho','Agosto','Setembro','Outubro','Novembro','Dezembro'];
const DIAS_SEMANA_ABREV = ['Dom','Seg','Ter','Qua','Qui','Sex','Sáb'];

interface DadosDia {
  totalMins: number;
  materias: { nome: string; cor: string; mins: number }[];
}

interface TooltipHeatmap {
  dateStr: string;
  dados: DadosDia | null;
  mouseX: number;
  mouseY: number;
}

type EntradaGrafico = {
  name: string;
  color: string;
  totalHours: number;
  sessionDates: string[];
  sessionDurations: number[];
} & Record<string, unknown>;

// Tooltip customizado do gráfico empilhado
interface PropsTooltipGrafico {
  active?: boolean;
  payload?: {
    name: string;
    value: number;
    color: string;
    payload: {
      color: string;
      sessionDates: string[];
      sessionDurations: number[];
    };
  }[];
  label?: string;
}

function TooltipGrafico({ active, payload, label }: PropsTooltipGrafico) {
  if (!active || !payload || payload.length === 0) return null;
  
  const mInfo = payload[0];
  const totalSubjHours = mInfo.value;
  const rawPayload = mInfo.payload;
  const listSessoes = rawPayload.sessionDates || [];
  
  const formatarDuracao = (mins: number) => {
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    if (h === 0) return `${m}min`;
    if (m === 0) return `${h}h`;
    return `${h}h ${m}min`;
  };

  const formatarDataLocal = (dateStr: string) => {
    const [year, month, day] = dateStr.split('-');
    return `${day}/${month}/${year}`;
  };

  return (
    <div className="heatmap-tooltip" style={{ position: 'static', transform: 'none', minWidth: '220px' }}>
      <div className="heatmap-tooltip-data" style={{ borderBottom: '1px solid var(--border-color)', paddingBottom: '4px', marginBottom: '6px' }}>
        {label}
      </div>
      <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', marginBottom: '6px' }}>
        Total Acumulado: <strong style={{ color: 'var(--primary)' }}>{totalSubjHours.toFixed(1)}h</strong>
      </div>
      {listSessoes.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '3px', maxHeight: '120px', overflowY: 'auto', paddingRight: '4px' }}>
          {listSessoes.map((data, idx) => (
            <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.72rem', color: 'var(--text-muted)' }}>
              <span>{formatarDataLocal(data)}</span>
              <strong>{formatarDuracao(rawPayload.sessionDurations[idx])}</strong>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default function Analytics() {
  const [celulaHover, setCelulaHover] = useState<{ barIdx: number; subjIdx: number } | null>(null);
  const celulaHoverRef = useRef(celulaHover);
  celulaHoverRef.current = celulaHover;

  const [heatmapTooltip, setHeatmapTooltip] = useState<TooltipHeatmap | null>(null);

  // Queries
  const { data: sessions = [], isLoading: loadingSessions } = useQuery<StudySession[]>({
    queryKey: ['sessions'],
    queryFn: async () => (await apiClient.get<StudySession[]>('/api/study-sessions')).data,
  });

  const { data: subjects = [], isLoading: loadingSubjects } = useQuery<Subject[]>({
    queryKey: ['subjects'],
    queryFn: async () => (await apiClient.get<Subject[]>('/api/subjects')).data,
  });

  const isLoading = loadingSessions || loadingSubjects;

  const formatarDuracao = (mins: number): string => {
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    if (h === 0) return `${m}min`;
    if (m === 0) return `${h}h`;
    return `${h}h ${m}min`;
  };

  // ─── Dados do gráfico empilhado ───────────────────────────────────────
  const dadosBase: EntradaGrafico[] = subjects
    .map(subj => {
      const sessoesOrdenadas = sessions
        .filter(s => s.subject?.id === subj.id)
        .sort((a, b) => new Date(a.sessionDate).getTime() - new Date(b.sessionDate).getTime());
      if (sessoesOrdenadas.length === 0) return null;
      return {
        name: subj.subjectName,
        color: subj.color || '#6366f1',
        totalHours: Number((sessoesOrdenadas.reduce((acc, s) => acc + s.duration, 0) / 60).toFixed(2)),
        sessionDates: sessoesOrdenadas.map(s => s.sessionDate),
        sessionDurations: sessoesOrdenadas.map(s => s.duration),
      };
    })
    .filter(Boolean) as EntradaGrafico[];

  const maxSessoes = Math.max(...dadosBase.map(d => d.sessionDates.length), 0);
  const dadosGrafico = dadosBase.map(subjData => {
    const entry: Record<string, unknown> = {
      name: subjData.name,
      color: subjData.color,
      sessionDates: subjData.sessionDates,
      sessionDurations: subjData.sessionDurations,
      totalHours: subjData.totalHours,
    };
    subjData.sessionDurations.forEach((dur, idx) => {
      entry[`session_${idx}`] = Number((dur / 60).toFixed(2));
    });
    return entry;
  });

  // ─── Dados do heatmap ─────────────────────────────────────────────────
  const hoje = new Date();
  const anoAtual = hoje.getFullYear();
  const mesAtual = hoje.getMonth();
  const hojeStr = `${anoAtual}-${String(mesAtual + 1).padStart(2, '0')}-${String(hoje.getDate()).padStart(2, '0')}`;

  const mapaDias = new Map<string, DadosDia>();
  sessions.forEach(s => {
    if (!s.sessionDate) return;
    const [ano, mes] = s.sessionDate.split('-').map(Number);
    if (ano !== anoAtual || mes - 1 !== mesAtual) return;
    const atual = mapaDias.get(s.sessionDate) ?? { totalMins: 0, materias: [] };
    atual.totalMins += s.duration;
    const nome = s.subject?.subjectName ?? '—';
    const cor = s.subject?.color ?? 'var(--primary)';
    const matIdx = atual.materias.findIndex(m => m.nome === nome);
    if (matIdx >= 0) atual.materias[matIdx].mins += s.duration;
    else atual.materias.push({ nome, cor, mins: s.duration });
    mapaDias.set(s.sessionDate, atual);
  });

  const maxMinsNoMes = Math.max(...Array.from(mapaDias.values()).map(d => d.totalMins), 1);
  const diasNoMes = new Date(anoAtual, mesAtual + 1, 0).getDate();
  const primeiroDiaSemana = new Date(anoAtual, mesAtual, 1).getDay();

  const corCelulaDia = (mins: number): string => {
    const ratio = mins / maxMinsNoMes;
    if (ratio <= 0) return 'transparent';
    if (ratio < 0.25) return 'rgba(99,102,241,0.25)';
    if (ratio < 0.5)  return 'rgba(99,102,241,0.48)';
    if (ratio < 0.75) return 'rgba(99,102,241,0.72)';
    return 'rgba(99,102,241,0.95)';
  };

  const celulasHeatmap: ({ tipo: 'padding' } | { tipo: 'dia'; dia: number; dateStr: string })[] = [
    ...Array.from({ length: primeiroDiaSemana }, () => ({ tipo: 'padding' as const })),
    ...Array.from({ length: diasNoMes }, (_, i) => {
      const dia = i + 1;
      const dateStr = `${anoAtual}-${String(mesAtual + 1).padStart(2, '0')}-${String(dia).padStart(2, '0')}`;
      return { tipo: 'dia' as const, dia, dateStr };
    }),
  ];
  const resto = celulasHeatmap.length % 7;
  if (resto !== 0) {
    for (let i = 0; i < 7 - resto; i++) celulasHeatmap.push({ tipo: 'padding' });
  }

  // Estatísticas Analíticas Adicionais
  const totalMinsGlobal = sessions.reduce((acc, s) => acc + s.duration, 0);
  const mediaSessao = sessions.length > 0 ? Math.round(totalMinsGlobal / sessions.length) : 0;
  const materiaMaisEstudada = () => {
    if (sessions.length === 0 || subjects.length === 0) return 'Nenhuma';
    const sumMap: Record<string, number> = {};
    sessions.forEach(s => {
      if (s.subject) {
        sumMap[s.subject.subjectName] = (sumMap[s.subject.subjectName] || 0) + s.duration;
      }
    });
    let maxName = 'Nenhuma';
    let maxVal = 0;
    Object.entries(sumMap).forEach(([name, val]) => {
      if (val > maxVal) {
        maxVal = val;
        maxName = name;
      }
    });
    return maxName;
  };

  if (isLoading) {
    return (
      <div className="flex-center" style={{ minHeight: '400px', color: 'var(--text-secondary)' }}>
        Carregando gráficos de evolução...
      </div>
    );
  }

  return (
    <div className="dashboard-root" style={{ animation: 'fadeIn 0.4s ease-out' }}>
      <div className="title-section">
        <div>
          <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
            <TrendingUp size={28} className="text-primary" />
            Evolução & Desempenho
          </h1>
          <p className="subtitle">Lógica reflexiva e análise científica de progresso</p>
        </div>
      </div>

      {/* Grid de Insights Analíticos */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon" style={{ backgroundColor: 'var(--primary-glow)', color: 'var(--primary)' }}>
            <Clock size={24} />
          </div>
          <div className="stat-info">
            <h3>Duração Média por Sessão</h3>
            <p>{mediaSessao} min</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ backgroundColor: 'rgba(16, 185, 129, 0.15)', color: '#10b981' }}>
            <Award size={24} />
          </div>
          <div className="stat-info">
            <h3>Matéria Mais Focada</h3>
            <p style={{ fontSize: '1.25rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '180px' }}>
              {materiaMaisEstudada()}
            </p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ backgroundColor: 'rgba(245, 158, 11, 0.15)', color: '#f59e0b' }}>
            <BookOpen size={24} />
          </div>
          <div className="stat-info">
            <h3>Total de Sessões Gravadas</h3>
            <p>{sessions.length}</p>
          </div>
        </div>
      </div>

      <div className="dashboard-grid">
        {/* Coluna Esquerda: Histórico e Distribuição de Horas */}
        <div className="dashboard-coluna-esquerda">
          <div className="card">
            <h2 className="card-title" style={{ marginBottom: '1.5rem' }}>
              Distribuição Acumulada por Matéria
            </h2>

            {dadosGrafico.length === 0 ? (
              <div className="flex-center" style={{ height: '300px', color: 'var(--text-muted)', fontSize: '0.875rem' }}>
                Grave sessões de estudo para analisar a distribuição.
              </div>
            ) : (
              <div style={{ width: '100%', height: '320px' }}>
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={dadosGrafico} margin={{ top: 8, right: 8, left: -28, bottom: 0 }}>
                    <XAxis dataKey="name" stroke="var(--text-muted)" fontSize={11} tickLine={false} tick={{ fill: 'var(--text-secondary)' }} />
                    <YAxis stroke="var(--text-muted)" fontSize={11} tickLine={false} axisLine={false} tick={{ fill: 'var(--text-muted)' }} unit="h" />
                    <Tooltip
                      content={(props) => (
                        <TooltipGrafico
                          active={props.active}
                          payload={props.payload as any}
                          label={props.label as string}
                        />
                      )}
                      cursor={{ fill: 'rgba(255,255,255,0.03)' }}
                    />

                    {Array.from({ length: maxSessoes }, (_, barIdx) => (
                      <Bar
                        key={`bar-${barIdx}`}
                        dataKey={`session_${barIdx}`}
                        stackId="a"
                        radius={barIdx === maxSessoes - 1 ? [4, 4, 0, 0] : [0, 0, 0, 0]}
                        isAnimationActive={false}
                        onMouseEnter={(_, subjIdx) => setCelulaHover({ barIdx, subjIdx })}
                        onMouseLeave={() => setCelulaHover(null)}
                      >
                        {dadosGrafico.map((entrada, subjIdx) => {
                          const hover = celulaHoverRef.current;
                          const estaHover = hover !== null && hover.barIdx === barIdx && hover.subjIdx === subjIdx;
                          return (
                            <Cell
                              key={`cell-${subjIdx}-${barIdx}`}
                              fill={entrada.color as string}
                              fillOpacity={barIdx % 2 === 0 ? 1 : 0.65}
                              stroke={estaHover ? 'rgba(255,255,255,0.9)' : 'rgba(0,0,0,0.1)'}
                              strokeWidth={estaHover ? 2 : 0}
                            />
                          );
                        })}
                      </Bar>
                    ))}
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </div>
        </div>

        {/* Coluna Direita: Calendário Heatmap */}
        <div className="dashboard-coluna-direita">
          <div className="card heatmap-card">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.25rem' }}>
              <Calendar size={18} className="text-primary" />
              <h2 className="card-title" style={{ margin: 0 }}>
                Evolução Diária ({MESES_PT[mesAtual]} {anoAtual})
              </h2>
            </div>

            {/* Dias da semana */}
            <div className="heatmap-grid" style={{ marginBottom: '8px' }}>
              {DIAS_SEMANA_ABREV.map(d => (
                <div key={d} className="heatmap-dia-semana">{d}</div>
              ))}
            </div>

            {/* Grid dos dias */}
            <div className="heatmap-grid">
              {celulasHeatmap.map((celula, idx) => {
                if (celula.tipo === 'padding') {
                  return <div key={`pad-${idx}`} className="heatmap-celula heatmap-celula--padding" />;
                }
                const dados = mapaDias.get(celula.dateStr) ?? null;
                const temDados = dados !== null && dados.totalMins > 0;
                const eHoje = celula.dateStr === hojeStr;
                return (
                  <div
                    key={celula.dateStr}
                    className={[
                      'heatmap-celula',
                      temDados ? 'heatmap-celula--com-dados' : '',
                      eHoje ? 'heatmap-celula--hoje' : '',
                    ].join(' ')}
                    style={{ backgroundColor: temDados ? corCelulaDia(dados!.totalMins) : undefined }}
                    onMouseEnter={e => setHeatmapTooltip({ dateStr: celula.dateStr, dados, mouseX: e.clientX, mouseY: e.clientY })}
                    onMouseMove={e => setHeatmapTooltip(t => t ? { ...t, mouseX: e.clientX, mouseY: e.clientY } : null)}
                    onMouseLeave={() => setHeatmapTooltip(null)}
                  >
                    {celula.dia}
                  </div>
                );
              })}
            </div>

            <div className="heatmap-legenda" style={{ marginTop: '1.25rem' }}>
              <span>Pouco</span>
              {['rgba(99,102,241,0.2)','rgba(99,102,241,0.45)','rgba(99,102,241,0.7)','rgba(99,102,241,0.95)'].map((cor, i) => (
                <div key={i} className="heatmap-legenda-celula" style={{ backgroundColor: cor }} />
              ))}
              <span>Muito</span>
            </div>
          </div>
        </div>
      </div>

      {/* Tooltip do Heatmap */}
      {heatmapTooltip && (
        <div
          className="heatmap-tooltip"
          style={{ left: heatmapTooltip.mouseX, top: heatmapTooltip.mouseY, position: 'fixed' }}
        >
          <div className="heatmap-tooltip-data">
            {(() => {
              const [ano, mes, dia] = heatmapTooltip.dateStr.split('-');
              return `${dia}/${mes}/${ano}`;
            })()}
          </div>
          {heatmapTooltip.dados && heatmapTooltip.dados.totalMins > 0 ? (
            <>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginBottom: '5px' }}>
                Total: <strong style={{ color: 'var(--text-primary)' }}>{formatarDuracao(heatmapTooltip.dados.totalMins)}</strong>
              </div>
              {heatmapTooltip.dados.materias.map(m => (
                <div key={m.nome} style={{ display: 'flex', justifyContent: 'space-between', gap: '8px', fontSize: '0.72rem', color: 'var(--text-secondary)', marginTop: '2px' }}>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <span style={{ width: '7px', height: '7px', borderRadius: '50%', backgroundColor: m.cor, flexShrink: 0 }} />
                    {m.nome}
                  </span>
                  <span>{formatarDuracao(m.mins)}</span>
                </div>
              ))}
            </>
          ) : (
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Nenhum estudo registrado</div>
          )}
        </div>
      )}
    </div>
  );
}
