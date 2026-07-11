/**
 * Dispara uma animação de confetes em tela cheia usando HTML/CSS puro.
 */
export function triggerConfetti() {
  const container = document.createElement('div');
  container.className = 'confetti-container';
  document.body.appendChild(container);
  
  const colors = ['#f59e0b', '#10b981', '#6366f1', '#ec4899', '#3b82f6', '#ef4444'];
  
  for (let i = 0; i < 80; i++) {
    const particle = document.createElement('div');
    particle.className = 'confetti-particle';
    particle.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];
    particle.style.left = Math.random() * 100 + 'vw';
    particle.style.width = Math.random() * 8 + 6 + 'px';
    particle.style.height = Math.random() * 12 + 8 + 'px';
    particle.style.animationDelay = Math.random() * 0.8 + 's';
    particle.style.animationDuration = Math.random() * 1.5 + 2 + 's';
    particle.style.transform = `rotate(${Math.random() * 360}deg)`;
    container.appendChild(particle);
  }
  
  setTimeout(() => {
    container.remove();
  }, 4000);
}
