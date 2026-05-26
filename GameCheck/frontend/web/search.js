const GAMES_PER_PAGE = 10; 
const API_BASE_URL = 'https://team-pura-final-project-production.up.railway.app/api';
let currentPage = 1;
let filteredGames = [];
let cachedGames = []; 
// ── DOM ────────────────────────────────────────────────────
const grid         = document.getElementById('resultsGrid');
const paginationEl = document.getElementById('pagination');
const countEl      = document.getElementById('resultsCount');
const searchInput  = document.getElementById('searchInput');
const filterPlatform = document.getElementById('filterPlatform');
const filterFormat = document.getElementById('filterFormat');
const loadingText  = document.getElementById('loadingText');

// ── Render Cards ───────────────────────────────────────────
function renderGames(games) 
{
  grid.innerHTML = '';

  if(games.length === 0) 
  {
    grid.innerHTML = '<p class="no-results">No games match your search.</p>';
    return;
  }

  const start = (currentPage - 1) * GAMES_PER_PAGE;
  const slice = games.slice(start, start + GAMES_PER_PAGE);

  slice.forEach((game, i) => 
  {
    const card = document.createElement('div');
    card.className = 'game-card';
    card.style.animationDelay = `${i * 0.05}s`;
    card.addEventListener('click', () => 
    {
      window.location.href = `gamePage.html?id=${game.gameId}`;
    });

    const coverImage = game.coverImageUrl || '../svg/gamePic.svg';
    card.innerHTML = `
      <div class="game-card-img" style="background-image: url('${coverImage}'); background-size: cover; background-position: center;"></div>
      <div class="game-card-title">${game.title}</div>
      <div class="game-card-meta">${game.platform}</div>
    `;
    grid.appendChild(card);
  });
}

// ── Render Pagination ──────────────────────────────────────
function renderPagination(total) 
{
  paginationEl.innerHTML = '';
  const totalPages = Math.ceil(total / GAMES_PER_PAGE);

  if(totalPages <= 1) 
  {
    paginationEl.classList.add('hidden');
    return;
  }
  paginationEl.classList.remove('hidden');

  // Logic to calculate which page numbers to show
  const delta = 1;
  const range = [];
  for(let i = Math.max(2, currentPage - delta); i <= Math.min(totalPages - 1, currentPage + delta); i++) 
  {
    range.push(i);
  }

  if(currentPage - delta > 2) 
  {
    range.unshift('...');
  }

  if(currentPage + delta < totalPages - 1) 
  {
    range.push('...');
  }
  range.unshift(1);
  range.push(totalPages);
  range.forEach(p => 
  {
    if(p === '...') 
    {
      const dots = document.createElement('span');
      dots.className = 'page-dots';
      dots.textContent = '...';
      paginationEl.appendChild(dots);
    } 
    
    else 
    {
      const btn = document.createElement('button');
      btn.className = 'page-num' + (p === currentPage ? ' active' : '');
      btn.textContent = p;
      btn.addEventListener('click', () => 
      {
        currentPage = p;
        renderGames(filteredGames);
        renderPagination(filteredGames.length);
        updateCount(filteredGames.length);
        document.querySelector('.results-section').scrollIntoView({ behavior: 'smooth' });
      });
      paginationEl.appendChild(btn);
    }
  });
}

// ── Update Count Label ─────────────────────────────────────
function updateCount(total) 
{
  const start = (currentPage - 1) * GAMES_PER_PAGE + 1;
  const end   = Math.min(currentPage * GAMES_PER_PAGE, total);
  countEl.textContent = total > 0
    ? `Showing ${start}–${end} of ${total} results`
    : '';
}

// ── Unified Filter Pipeline ─────────────────────────────────── //
function filterGames(games, query, platform, format) 
{
  const queryClean = query
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();
  
  return games.filter(game => 
  {
    const gameTitleClean = (game.title || '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase();

    // Text matching: if query is empty, allow all games to pass
    const matchesText = queryClean === '' 
      ? true 
      : gameTitleClean.includes(queryClean);
    
    // Platform filter: bypass placeholder values
    const matchesPlatform = (platform === 'Platform' || platform === '' || platform === 'all')
      ? true
      : game.platform.toLowerCase() === platform.toLowerCase();
    
    // Format filter: bypass placeholder values, check if game has the format
    const matchesFormat = (format === 'Format' || format === '' || format === 'all')
      ? true
      : game.availableFormats && game.availableFormats.some(f => f.toLowerCase() === format.toLowerCase());
    
    return matchesText && matchesPlatform && matchesFormat;
  });
}

// ── Apply Filters (100% Local Filtering) ─────────────────────
function applyFilters()
{
  const query = searchInput.value.trim();
  const platform = filterPlatform ? filterPlatform.value : 'all';
  const format = filterFormat ? filterFormat.value : 'all';

  filteredGames = filterGames(cachedGames, query, platform, format);
  currentPage = 1;
  renderGames(filteredGames);
  renderPagination(filteredGames.length);
  updateCount(filteredGames.length);
}

// ── Event Listeners ──────────────────────────────────────── //
searchInput.addEventListener('input', applyFilters);
if(filterPlatform) 
{
  filterPlatform.addEventListener('change', applyFilters);
}

if(filterFormat) 
{
  filterFormat.addEventListener('change', applyFilters);
}

document.querySelectorAll('img, svg, video').forEach(el => 
{
  el.setAttribute('draggable', 'false');
  el.addEventListener('dragstart', e => e.preventDefault());
});

document.querySelectorAll('img').forEach(el => 
{
  el.addEventListener('contextmenu', e => e.preventDefault());
});

// ── Initial Load ───────────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => 
{
  searchInput.value = ''; 
  
  if(loadingText) loadingText.style.display = 'none';

  const savedCatalog = sessionStorage.getItem('gamecheck_catalog');
  if (savedCatalog) 
    {
    cachedGames = JSON.parse(savedCatalog);
    applyFilters(); 
    return;
  }

  let skeletonHTML = '';
  for (let i = 0; i < 10; i++) 
  {
    skeletonHTML += `
      <div class="skeleton-card">
        <div class="skeleton-img"></div>
        <div class="skeleton-text"></div>
        <div class="skeleton-text" style="width: 50%;"></div>
      </div>
    `;
  }
  grid.innerHTML = skeletonHTML;

  try 
  {
    const response = await fetch(`${API_BASE_URL}/games/search?query=`);
    
    if(!response.ok) throw new Error(`API error: ${response.status}`);
    const data = await response.json();
    
    cachedGames = data;
    sessionStorage.setItem('gamecheck_catalog', JSON.stringify(data));

    applyFilters();

  } 
  
  catch(error) 
  {
    console.error('Failed to fetch games:', error);
    grid.innerHTML = '<p class="no-results" style="color: white;">Failed to load games. Please try again later.</p>';
  }
});