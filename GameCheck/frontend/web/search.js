// ── Config ─────────────────────────────────────────────────
    const GAMES_PER_PAGE = 10; // 5 columns × 2 rows
    const API_BASE_URL = 'https://team-pura-final-project-production.up.railway.app/api';
    let currentPage = 1;
    let filteredGames = [];
    let isLoading = false;

    // ── DOM ────────────────────────────────────────────────────
    const grid         = document.getElementById('resultsGrid');
    const paginationEl = document.getElementById('pagination');
    const countEl      = document.getElementById('resultsCount');
    const searchInput  = document.getElementById('searchInput');
    const filterPlatform = document.getElementById('filterPlatform');
    const filterFormat = document.getElementById('filterFormat');

    // ── Render Cards ───────────────────────────────────────────
    function renderGames(games) {
      grid.innerHTML = '';

      if (isLoading) {
        grid.innerHTML = '<p class="loading">Loading...</p>';
        return;
      }

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

    // ── Apply Filters (API Call + Client-side Filtering) ─────────
    async function applyFilters() {
      const query = searchInput.value.trim();
      const platform = filterPlatform ? filterPlatform.value : 'all';
      const format = filterFormat ? filterFormat.value : 'all';

      isLoading = true;
      renderGames([]); // Show loading state

      try {
        // Build API URL - only send query, do client-side filtering for platform/format
        const params = new URLSearchParams();
        params.append('query', query || '');

        const response = await fetch(`${API_BASE_URL}/games/search?${params.toString()}`);

        if (!response.ok) {
          throw new Error(`API error: ${response.status}`);
        }

        const data = await response.json();

        // Apply client-side filtering with flexible string matching
        filteredGames = data.filter(game => {
          // Platform filter - case-insensitive inclusion check
          if (platform && platform !== 'all' && platform !== 'Platform') {
            const gamePlatform = (game.platform || '').toLowerCase();
            const selectedPlatform = platform.toLowerCase();
            if (!gamePlatform.includes(selectedPlatform)) {
              return false;
            }
          }

          // Format filter - case-insensitive inclusion check
          if (format && format !== 'all' && format !== 'Format') {
            const gameFormat = (game.format || '').toLowerCase();
            const selectedFormat = format.toLowerCase();
            if (!gameFormat.includes(selectedFormat)) {
              return false;
            }
          }

          return true;
        });

        isLoading = false; // Clear loading state before rendering
        currentPage = 1;
        renderGames(filteredGames);
        renderPagination(filteredGames.length);
        updateCount(filteredGames.length);

      } catch (error) {
        console.error('Failed to fetch games:', error);
        isLoading = false; // Clear loading state on error
        grid.innerHTML = '<p class="error">Failed to load games. Please try again later.</p>';
        filteredGames = [];
        renderPagination(0);
        updateCount(0);
      }
    }

    // ── Event Listeners ────────────────────────────────────────
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
    // Load default games on page load
    document.addEventListener('DOMContentLoaded', () => {
      searchInput.value = ''; // Start with empty query
      applyFilters(); // Trigger initial fetch
    });