const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');

const port = 3000;

const server = http.createServer((req, res) => {
  // Define your exact frontend directory path
  const frontendDirectory = path.join(__dirname, 'frontend');

  // Strip query parameters from URL before processing
  let requestedPath = req.url.split('?')[0];
  // Decode URI to handle spaces in file names safely
  requestedPath = decodeURI(requestedPath); 
  
  if (requestedPath === '/') {
    requestedPath = '/web/hero.html'; 
  }

  // Combine the frontend folder path with whatever the browser is asking for
  const absolutePath = path.join(frontendDirectory, requestedPath);

  // Identify file type
  const extname = String(path.extname(absolutePath)).toLowerCase();
  
  // This list allows HTML, CSS, Images, and JS to load perfectly
  const mimeTypes = {
    '.html': 'text/html',
    '.js': 'text/javascript', // <-- This allows script.js to work!
    '.css': 'text/css',
    '.svg': 'image/svg+xml', 
    '.png': 'image/png',
    '.jpg': 'image/jpeg'
  };
  const contentType = mimeTypes[extname] || 'application/octet-stream';

  // Read and serve the file
  fs.readFile(absolutePath, (error, content) => {
    if (error) {
      // This console.log is your best friend. It prints EXACTLY where it tried to look!
      console.log("Failed to find:", absolutePath); 
      res.writeHead(404);
      res.end('File not found!');
    } else {
      res.writeHead(200, { 'Content-Type': contentType });
      res.end(content, 'utf-8');
    }
  });
});

server.listen(port, () => {
  console.log(`Server running at http://localhost:${port}/`);
});