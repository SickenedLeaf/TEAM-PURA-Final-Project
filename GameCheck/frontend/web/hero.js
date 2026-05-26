// ── Sticky Header Scroll Logic ──
const header = document.querySelector('.header');
window.addEventListener('scroll', () => 
{
  if (window.scrollY >= 24) {
    header.classList.add('is-sticky');
  } else {
    header.classList.remove('is-sticky');
  }
});

// ── Scroll fade-in via IntersectionObserver ──
const observer = new IntersectionObserver((entries) => 
{
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      entry.target.classList.add('visible');
      observer.unobserve(entry.target);
    }
  });
}, { threshold: 0.15 });

document.querySelectorAll('.fade-in').forEach(el => observer.observe(el));

// ── Block drag on all media ──
document.querySelectorAll('img, svg, video').forEach(el => 
{
  el.setAttribute('draggable', 'false');
  el.addEventListener('dragstart', e => e.preventDefault());
});

// ── Block right-click context menu on images ──
document.querySelectorAll('img').forEach(el => 
{
  el.addEventListener('contextmenu', e => e.preventDefault());
});

const homeBtn = document.querySelector('.nav-btn[alt="Home"]');
if(homeBtn) 
{
  homeBtn.addEventListener('click', () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  });
}

// ── Smooth Scrolling for Navigation ──
const aboutBtn = document.querySelector('.nav-btn[alt="About Us"]');
const aboutSection = document.querySelector('.about-us-section');

if(aboutBtn && aboutSection) 
{
  aboutBtn.addEventListener('click', () => 
  {
    const headerOffset = 55;
    const elementPosition = aboutSection.getBoundingClientRect().top;
    const offsetPosition = elementPosition + window.scrollY - headerOffset;
    window.scrollTo({ top: offsetPosition, behavior: 'smooth' });
  });
}

// ── Navigate to Search Page via CTA Button ──
const ctaBtn = document.querySelector('.cta-btn-svg');
if(ctaBtn) 
{
  ctaBtn.addEventListener('click', () => {
    window.location.href = '/web/search.html';
  });
}

// ── Carousel ──────────────────────────────────────────────────────
const CAROUSEL_IMAGES = [
  '../assets/1.jpg',
  '../assets/2.jpg',
  '../assets/3.jpg',
  '../assets/4.png',
  '../assets/5.jpg',
];
const SLIDE_DURATION = 5000; // how long each photo shows (ms)
const FADE_DURATION  = 800;  // crossfade speed (ms) — must match hero.css transition

const carouselImg = document.querySelector('.carousel-picture-bar');
if(carouselImg) 
{
  CAROUSEL_IMAGES.forEach(src => { new Image().src = src; });
  let current = 0;
  setInterval(() => 
    {
    const next = (current + 1) % CAROUSEL_IMAGES.length;
    carouselImg.style.opacity = '0';

    setTimeout(() => 
    {
      carouselImg.src = CAROUSEL_IMAGES[next];
      carouselImg.style.opacity = '1';
      current = next;
    }, FADE_DURATION);

  }, SLIDE_DURATION);
}