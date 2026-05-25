// ── Config ─────────────────────────────────────────────────
const API_BASE_URL = 'https://team-pura-final-project-production.up.railway.app/api';

// ── Get Game ID from URL ───────────────────────────────────
const urlParams = new URLSearchParams(window.location.search);
const gameId = urlParams.get('id');

// ── Sticky Header Scroll Logic ──
const header = document.querySelector('.header');

if (header) {
  window.addEventListener('scroll', () => {
    if (window.scrollY >= 24) {
      header.classList.add('is-sticky');
    } else {
      header.classList.remove('is-sticky');
    }
  });
}

// ── Summon & Retract Words Section Logic ──
const tabBtns = document.querySelectorAll('.tab-btn');
const wordsSection = document.getElementById('wordsSection');
const tabPanes = document.querySelectorAll('.tab-pane');

tabBtns.forEach(btn => {
  btn.addEventListener('click', () => {
    const targetId = btn.getAttribute('data-target');
    const isAlreadyActive = btn.classList.contains('active');

    if (isAlreadyActive) {
      // RULE: If clicked twice, trigger the fade-out animation
      btn.classList.remove('active');
      wordsSection.classList.add('closing');
      
      // Wait for the CSS fade-out animation (0.4s) to finish before hiding
      setTimeout(() => {
        wordsSection.classList.remove('visible');
        wordsSection.classList.remove('closing');
        tabPanes.forEach(p => p.classList.remove('active'));
      }, 400); 

    } else {
      // RULE: Clicking a new tab
      
      // 1. Reset all buttons and panes
      tabBtns.forEach(b => b.classList.remove('active'));
      tabPanes.forEach(p => p.classList.remove('active'));
      
      // (Cancel closing animation if they clicked really fast)
      wordsSection.classList.remove('closing');

      // 2. Activate the clicked button and matching pane
      btn.classList.add('active');
      const targetPane = document.getElementById(targetId);
      if (targetPane) targetPane.classList.add('active');

      // 3. Slide up the section if it's currently hidden
      if (!wordsSection.classList.contains('visible')) {
        wordsSection.classList.add('visible');
        
        // Smooth scroll to the section
        setTimeout(() => {
          wordsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }, 50);
      }
    }
  });
});

// ── Prevent Image Dragging ──
document.querySelectorAll('img, svg').forEach(el => {
  el.setAttribute('draggable', 'false');
  el.addEventListener('dragstart', e => e.preventDefault());
  el.addEventListener('contextmenu', e => e.preventDefault());
});

// ── Load Game Data ────────────────────────────────────────────
async function loadGameData() {
  if (!gameId) {
    console.error('No game ID provided in URL');
    document.querySelector('.game-title').textContent = 'Game Not Found';
    return;
  }

  // 1. INSTANT CACHE LOAD: Prevent the ugly placeholder flash
  const savedCatalog = sessionStorage.getItem('gamecheck_catalog');
  if (savedCatalog) {
    const catalog = JSON.parse(savedCatalog);
    // Find the game in the cache (using == in case of string/int mismatch)
    const cachedGame = catalog.find(g => g.gameId == gameId || g.id == gameId);
    if (cachedGame) {
      populateGameDetails(cachedGame); // Injects Title & Image instantly!
    }
  } else {
    // If they opened the link directly (no cache), clear the "Game" text so it doesn't flash
    document.querySelector('.game-title').textContent = 'Loading...';
  }

  // 2. Set Price Table to a loading state so it's not just empty
  const tbody = document.getElementById('priceTableBody');
  if (tbody) {
    tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; opacity: 0.5;">Fetching latest prices...</td></tr>';
  }

  try {
    // 3. Fetch fresh game details (just in case the database updated in the last 5 minutes)
    const gameResponse = await fetch(`${API_BASE_URL}/games/${gameId}`);
    if (!gameResponse.ok) throw new Error(`Failed to fetch game: ${gameResponse.status}`);
    const gameData = await gameResponse.json();
    populateGameDetails(gameData);

    // 4. Fetch the prices
    const pricesResponse = await fetch(`${API_BASE_URL}/games/${gameId}/prices`);
    if (!pricesResponse.ok) throw new Error(`Failed to fetch prices: ${pricesResponse.status}`);
    const pricesData = await pricesResponse.json();
    
    // 5. Inject the real prices!
    populatePriceTable(pricesData);
    updateAvailability(pricesData);

  } catch (error) {
    console.error('Failed to load game data:', error);
    document.querySelector('.game-title').textContent = 'Failed to load game data';
    if (tbody) {
      tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: #ff6b6b;">Error loading prices</td></tr>';
    }
  }
}

// ── Populate Game Details ─────────────────────────────────────
function populateGameDetails(game) {
  // Update title
  const titleEl = document.querySelector('.game-title');
  if (titleEl && game.title) {
    titleEl.textContent = game.title;
  }

  // Update cover image
  const coverImgEl = document.querySelector('.game-picture');
  if (coverImgEl && game.coverImageUrl) {
    coverImgEl.src = game.coverImageUrl;
  }

  // Update platforms
  const platformsEl = document.querySelector('.platform-tags');
  if (platformsEl && game.platform) {
    platformsEl.innerHTML = `<span class="tag">${game.platform}</span>`;
  }
}

// ── Update Availability Text ────────────────────────────────────
function updateAvailability(prices) {
  const availabilityText = document.getElementById('availabilityText');
  if (!availabilityText) return;

  if (!prices || prices.length === 0) {
    availabilityText.textContent = 'No availability information found for this title.';
    return;
  }

  const hasPhysical = prices.some(p => p.sourceType === 'physical');
  const hasDigital = prices.some(p => p.sourceType === 'digital');

  if (hasPhysical && hasDigital) {
    availabilityText.textContent = 'Available in physical and digital formats.';
  } else if (hasPhysical) {
    availabilityText.textContent = 'Available at physical stores.';
  } else if (hasDigital) {
    availabilityText.textContent = 'Available digitally.';
  } else {
    availabilityText.textContent = 'No availability information found for this title.';
  }
}

// ── Populate Price Table ───────────────────────────────────────
function populatePriceTable(prices) {
  const tbody = document.getElementById('priceTableBody');
  if (!tbody) return;

  tbody.innerHTML = '';

  if (!prices || prices.length === 0) {
    tbody.innerHTML = '<tr><td colspan="5">No price data available</td></tr>';
    return;
  }

  prices.forEach(price => {
    const row = document.createElement('tr');
    if (price.cheapest) {
      row.classList.add('cheapest');
    }

    const formattedPrice = price.pricePhp
      ? `₱${parseFloat(price.pricePhp).toLocaleString('en-PH', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
      : 'N/A';

    const lastUpdated = price.lastUpdated
      ? new Date(price.lastUpdated).toLocaleDateString()
      : 'N/A';

    row.innerHTML = `
      <td>${price.sourceName}</td>
      <td>${price.sourceType || 'N/A'}</td>
      <td>${formattedPrice}</td>
      <td><a href="${price.listingUrl || '#'}" target="_blank" rel="noopener noreferrer">View</a></td>
      <td>${lastUpdated}</td>
    `;
    tbody.appendChild(row);
  });
}

// ── Load data on page load ─────────────────────────────────────
loadGameData();