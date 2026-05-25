// ── Config ─────────────────────────────────────────────────
    const GAMES_PER_PAGE = 10; // 5 columns × 2 rows
    const API_BASE_URL = 'https://team-pura-final-project-production.up.railway.app/api';
    let currentPage = 1;
    let filteredGames = [];
    let cachedGames = []; // Cache for fetched games - fetch once, filter locally

    // ── DOM ────────────────────────────────────────────────────
    const grid         = document.getElementById('resultsGrid');
    const paginationEl = document.getElementById('pagination');
    const countEl      = document.getElementById('resultsCount');
    const searchInput  = document.getElementById('searchInput');
    const filterPlatform = document.getElementById('filterPlatform');
    const filterFormat = document.getElementById('filterFormat');
    const loadingText  = document.getElementById('loadingText');

    // ── Render Cards ───────────────────────────────────────────
    function renderGames(games) {
      grid.innerHTML = '';

      if (games.length === 0) {
        grid.innerHTML = '<p class="no-results">No games match your search.</p>';
        return;
      }

      // Slice for current page
      const start = (currentPage - 1) * GAMES_PER_PAGE;
      const slice = games.slice(start, start + GAMES_PER_PAGE);

      slice.forEach((game, i) => {
        const card = document.createElement('div');
        card.className = 'game-card';
        card.style.animationDelay = `${i * 0.05}s`;

        // ── Click listener to navigate to game details with gameId ──
        card.addEventListener('click', () => {
          window.location.href = `gamePage.html?id=${game.gameId}`;
        });

        // Format price as ₱X,XXX.XX
        const formattedPrice = game.bestPricePhp
          ? `₱${parseFloat(game.bestPricePhp).toLocaleString('en-PH', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
          : 'Price not available';

        // Use cover image or placeholder
        const coverImage = game.coverImageUrl || '../svg/gamePic.svg';

        card.innerHTML = `
          <div class="game-card-img" style="background-image: url('${coverImage}'); background-size: cover; background-position: center;"></div>
          <div class="game-card-title">${game.title}</div>
          <div class="game-card-price">${formattedPrice}</div>
          <div class="game-card-meta">${game.platform}</div>
        `;
        grid.appendChild(card);
      });
    }

    // ── Render Pagination ──────────────────────────────────────
    function renderPagination(total) {
      paginationEl.innerHTML = '';
      const totalPages = Math.ceil(total / GAMES_PER_PAGE);

      // Hide pagination if only 1 page
      if (totalPages <= 1) {
        paginationEl.classList.add('hidden');
        return;
      }
      paginationEl.classList.remove('hidden');

      // Logic to calculate which page numbers to show
      const delta = 1; // How many pages to show before and after the current page
      const range = [];
      
      // Calculate the middle range of buttons
      for (let i = Math.max(2, currentPage - delta); i <= Math.min(totalPages - 1, currentPage + delta); i++) {
        range.push(i);
      }

      // Add ellipses if there are gaps
      if (currentPage - delta > 2) {
        range.unshift('...');
      }
      if (currentPage + delta < totalPages - 1) {
        range.push('...');
      }

      // Always show the first and last page
      range.unshift(1);
      range.push(totalPages);

      // Render the buttons and ellipses
      range.forEach(p => {
        if (p === '...') {
          const dots = document.createElement('span');
          dots.className = 'page-dots';
          dots.textContent = '...';
          paginationEl.appendChild(dots);
        } else {
          const btn = document.createElement('button');
          btn.className = 'page-num' + (p === currentPage ? ' active' : '');
          btn.textContent = p;
          btn.addEventListener('click', () => {
            currentPage = p;
            renderGames(filteredGames);
            renderPagination(filteredGames.length);
            updateCount(filteredGames.length);
            // Scroll to results smoothly
            document.querySelector('.results-section').scrollIntoView({ behavior: 'smooth' });
          });
          paginationEl.appendChild(btn);
        }
      });
    }

    // ── Update Count Label ─────────────────────────────────────
    function updateCount(total) {
      const start = (currentPage - 1) * GAMES_PER_PAGE + 1;
      const end   = Math.min(currentPage * GAMES_PER_PAGE, total);
      countEl.textContent = total > 0
        ? `Showing ${start}–${end} of ${total} results`
        : '';
    }

    // ── Unified Filter Pipeline ───────────────────────────────────
    // Single source of truth for all filtering logic
    function filterGames(games, query, platform, format) {
      const queryLower = query.toLowerCase();
      
      return games.filter(game => {
        // Text matching: if query is empty, allow all games to pass
        const matchesText = queryLower === '' 
          ? true 
          : (game.title || '').toLowerCase().includes(queryLower);
        
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
    // Filters cachedGames locally - never makes network requests
    function applyFilters() {
      // Read all inputs simultaneously - single source of truth
      const query = searchInput.value.trim();
      const platform = filterPlatform ? filterPlatform.value : 'all';
      const format = filterFormat ? filterFormat.value : 'all';

      // Apply unified filter pipeline to cached games
      filteredGames = filterGames(cachedGames, query, platform, format);

      // Render results
      currentPage = 1;
      renderGames(filteredGames);
      renderPagination(filteredGames.length);
      updateCount(filteredGames.length);
    }

    // ── Event Listeners ────────────────────────────────────────
    // All events trigger instant local filtering (no debounce needed)
    searchInput.addEventListener('input', applyFilters);
    if (filterPlatform) {
      filterPlatform.addEventListener('change', applyFilters);
    }
    if (filterFormat) {
      filterFormat.addEventListener('change', applyFilters);
    }

    // Prevent drag on all images
    document.querySelectorAll('img, svg, video').forEach(el => {
      el.setAttribute('draggable', 'false');
      el.addEventListener('dragstart', e => e.preventDefault());
    });
    document.querySelectorAll('img').forEach(el => {
      el.addEventListener('contextmenu', e => e.preventDefault());
    });

    // ── Initial Load ───────────────────────────────────────────
    // Fetch once on init, then filter locally
    document.addEventListener('DOMContentLoaded', async () => {
      searchInput.value = ''; // Start with empty query

      // Show loading state
      if (loadingText) {
        loadingText.style.display = 'block';
      }
      grid.innerHTML = '';

      try {
        // Fetch entire game catalog once
        const response = await fetch(`${API_BASE_URL}/games/search?query=`);
        
        if (!response.ok) {
          throw new Error(`API error: ${response.status}`);
        }

        const data = await response.json();
        
        // Save to cache
        cachedGames = data;

        // Hide loading state
        if (loadingText) {
          loadingText.style.display = 'none';
        }

        // Render initial UI using local filtering
        applyFilters();

      } catch (error) {
        // Hide loading state on error
        if (loadingText) {
          loadingText.style.display = 'none';
        }
        
        console.error('Failed to fetch games:', error);
        grid.innerHTML = '<p class="error">Failed to load games. Please try again later.</p>';
      }
    });