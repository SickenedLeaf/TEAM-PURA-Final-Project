const API_BASE_URL = 'https://team-pura-final-project-production.up.railway.app/api';
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

tabBtns.forEach(btn => 
{
  btn.addEventListener('click', () => 
  {
    const targetId = btn.getAttribute('data-target');
    const isAlreadyActive = btn.classList.contains('active');

    if(isAlreadyActive) 
    {
      //If clicked twice, trigger the fade-out animation
      btn.classList.remove('active');
      wordsSection.classList.add('closing');

      setTimeout(() => 
      {
        wordsSection.classList.remove('visible');
        wordsSection.classList.remove('closing');
        tabPanes.forEach(p => p.classList.remove('active'));
      }, 400); 

    } 
    
    else 
    {
      //Clicking a new tab
      tabBtns.forEach(b => b.classList.remove('active'));
      tabPanes.forEach(p => p.classList.remove('active'));
      wordsSection.classList.remove('closing');

      btn.classList.add('active');
      const targetPane = document.getElementById(targetId);
      if(targetPane) targetPane.classList.add('active');

      if(!wordsSection.classList.contains('visible')) 
      {
        wordsSection.classList.add('visible');
        setTimeout(() => 
        {
          wordsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }, 50);
      }
    }
  });
});

// ── Prevent Image Dragging ──
document.querySelectorAll('img, svg').forEach(el => 
{
  el.setAttribute('draggable', 'false');
  el.addEventListener('dragstart', e => e.preventDefault());
  el.addEventListener('contextmenu', e => e.preventDefault());
});

// ── Load Game Data ────────────────────────────────────────────
async function loadGameData() 
{
  if(!gameId) 
  {
    console.error('No game ID provided in URL');
    document.querySelector('.game-title').textContent = 'Game Not Found';
    return;
  }

  //INSTANT CACHE LOAD
  const savedCatalog = sessionStorage.getItem('gamecheck_catalog');
  if(savedCatalog) 
  {
    const catalog = JSON.parse(savedCatalog);
    const cachedGame = catalog.find(g => g.gameId == gameId || g.id == gameId);

    if(cachedGame) 
    {
      populateGameDetails(cachedGame); 
    }
  } 
  
  else 
  {
    document.querySelector('.game-title').textContent = 'Loading...';
  }

  //Set Price Table to a loading state so it's not just empty
  const tbody = document.getElementById('priceTableBody');
  if(tbody) 
  {
    tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; opacity: 0.5;">Fetching latest prices...</td></tr>';
  }

  try 
  {
    //Fetch fresh game details (just in case the database updated in the last 5 minutes)
    const gameResponse = await fetch(`${API_BASE_URL}/games/${gameId}`);
    if(!gameResponse.ok) throw new Error(`Failed to fetch game: ${gameResponse.status}`);
    
    const gameData = await gameResponse.json();
    populateGameDetails(gameData);

    const pricesResponse = await fetch(`${API_BASE_URL}/games/${gameId}/prices`);
    if(!pricesResponse.ok) throw new Error(`Failed to fetch prices: ${pricesResponse.status}`);
    const pricesData = await pricesResponse.json();
    
    populatePriceTable(pricesData);
    updateAvailability(pricesData);

  } 
  
  catch(error) 
  {
    console.error('Failed to load game data:', error);
    document.querySelector('.game-title').textContent = 'Failed to load game data';
    
    if(tbody) 
    {
      tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: #ff6b6b;">Error loading prices</td></tr>';
    }
  }
}

// ── Populate Game Details ─────────────────────────────────────
function populateGameDetails(game) 
{
  // Update title
  const titleEl = document.querySelector('.game-title');
  if(titleEl && game.title) 
  {
    titleEl.textContent = game.title;
  }

  // Update cover image
  const coverImgEl = document.querySelector('.game-picture');
  if(coverImgEl && game.coverImageUrl) 
  {
    coverImgEl.src = game.coverImageUrl;
    
    //Blurred Background
    document.body.style.setProperty('--bg-image', `url('${game.coverImageUrl}')`);
  }

  // Update platforms
  const platformsEl = document.querySelector('.platform-tags');
  if(platformsEl && game.platform) 
  {
    platformsEl.innerHTML = `<span class="tag">${game.platform}</span>`;
  }
}

// ── Update Availability Text ────────────────────────────────────
function updateAvailability(prices) 
{
  const availabilityText = document.getElementById('availabilityText');
  if(!availabilityText) return;

  if(!prices || prices.length === 0) 
  {
    availabilityText.textContent = 'No availability information found for this title.';
    return;
  }

  const physicalStores = new Set();
  const digitalStores = new Set();

  prices.forEach(p => 
  {
    if(p.sourceType === 'physical' && p.sourceName) 
    {
      physicalStores.add(p.sourceName);
    }
    if(p.sourceType === 'digital' && p.sourceName) 
    {
      digitalStores.add(p.sourceName);
    }
  });

  const physicalArr = Array.from(physicalStores);
  const digitalArr = Array.from(digitalStores);
  const formatList = (arr) => 
  {
    if(arr.length === 0) return '';
    if(arr.length === 1) return arr[0];
    if(arr.length === 2) return `${arr[0]} and ${arr[1]}`;
    return `${arr.slice(0, -1).join(', ')}, and ${arr[arr.length - 1]}`;
  };

  let messageParts = [];
  if(physicalArr.length > 0) 
  {
    messageParts.push(`Physical copies available at [${formatList(physicalArr)}].`);
  }

  if(digitalArr.length > 0) 
  {
    messageParts.push(`Digital copies available at [${formatList(digitalArr)}].`);
  }

  if(messageParts.length > 0) 
  {
    availabilityText.textContent = messageParts.join('<br>');
  } 
  
  else 
  {
    availabilityText.textContent = 'No availability information found for this title.';
  }
}

// ── Populate Price Table ───────────────────────────────────────
function populatePriceTable(prices) {
  const tbody = document.getElementById('priceTableBody');
  if(!tbody) return;

  tbody.innerHTML = '';

  if(!prices || prices.length === 0) 
  {
    tbody.innerHTML = '<tr><td colspan="5">No price data available</td></tr>';
    return;
  }

  prices.forEach(price => 
  {
    const row = document.createElement('tr');
    if (price.cheapest) 
    {
      row.classList.add('cheapest');
    }

    const formattedPrice = price.pricePhp
      ? `₱${parseFloat(price.pricePhp).toLocaleString('en-PH', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
      : 'N/A';
    const lastUpdated = price.lastUpdated
      ? new Date(price.lastUpdated).toLocaleDateString()
      : 'N/A';

    const displayType = price.sourceType
      ? price.sourceType.charAt(0).toUpperCase() + price.sourceType.slice(1)
      : 'N/A';

    row.innerHTML = `
      <td>${price.sourceName}</td>
      <td>${displayType}</td>
      <td>${formattedPrice}</td>
      <td><a href="${price.listingUrl || '#'}" target="_blank" rel="noopener noreferrer">View</a></td>
      <td>${lastUpdated}</td>
    `;
    tbody.appendChild(row);
  });
}

// ── Load data on page load ─────────────────────────────────────
loadGameData();