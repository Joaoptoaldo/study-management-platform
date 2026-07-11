/**
 * Calcula o streak de dias consecutivos de estudo.
 * Analisa as datas das sessões e retorna o número de dias seguidos ativos.
 */
export function calcularStreak(sessions: { sessionDate: string }[]): number {
  if (!sessions || sessions.length === 0) return 0;
  
  // Extrai as datas no formato local YYYY-MM-DD e remove duplicatas
  const datasUnicas = Array.from(new Set(
    sessions.map(s => {
      const d = new Date(s.sessionDate);
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    })
  )).sort((a, b) => b.localeCompare(a)); // Ordena decrescente (mais recente primeiro)

  if (datasUnicas.length === 0) return 0;

  const hoje = new Date();
  const hojeStr = `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, '0')}-${String(hoje.getDate()).padStart(2, '0')}`;
  
  const ontem = new Date();
  ontem.setDate(ontem.getDate() - 1);
  const ontemStr = `${ontem.getFullYear()}-${String(ontem.getMonth() + 1).padStart(2, '0')}-${String(ontem.getDate()).padStart(2, '0')}`;

  const maisRecente = datasUnicas[0];
  
  // Se o dia mais recente de estudo não for hoje nem ontem, o streak é 0
  if (maisRecente !== hojeStr && maisRecente !== ontemStr) {
    return 0;
  }

  let streak = 1;
  for (let i = 0; i < datasUnicas.length - 1; i++) {
    const dataAtual = new Date(datasUnicas[i] + 'T00:00:00');
    const proximaData = new Date(datasUnicas[i + 1] + 'T00:00:00');
    
    // Calcula a diferença em dias
    const diffTime = Math.abs(dataAtual.getTime() - proximaData.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays === 1) {
      streak++;
    } else if (diffDays > 1) {
      break; // Quebrou a sequência
    }
  }
  
  return streak;
}
