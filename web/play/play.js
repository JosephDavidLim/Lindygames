// Runs the desktop Java game in the page. /app/ is the root of this website,
// and /files/ is storage that stays in this browser between visits.
const loading = document.getElementById("loading");

await cheerpjInit({
  version: 17,
  status: "splash",
  javaProperties: ["game.dir=/app/play", "save.file=/files/progress.txt"]
});
cheerpjCreateDisplay(720, 596, document.getElementById("stage"));
loading.hidden = true;
await cheerpjRunMain("game.GameApp", "/app/play/11-1.jar:/app/play/acm.jar");
