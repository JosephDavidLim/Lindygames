# Lindygames

The year is 2100. Assignments have become physical constructs, born from the fear of people around the world. People and AI are evolving to defeat them.

This repo holds the website and both games set in that world.

| Folder | What it is |
|---|---|
| `web/` | The website: plain HTML, CSS and JavaScript, with no build step |
| `games/11-1/` | **11/1**, a lane defense RPG about people |
| `games/lindefense/` | **Lindefense**, a tower defense game about AI, by Hanbo Song and Austin Yoo (MIT License) |

Both games keep their full commit history and their own READMEs.

## Deploy the website on Vercel
1. In Vercel, choose **Add New > Project** and import `JosephDavidLim/Lindygames`.
2. Leave the settings as they are. `vercel.json` tells Vercel to publish the `web/` folder with no build.
3. Press **Deploy**.

To preview it on your computer:

```
cd web
python3 -m http.server 8000
```

Then open http://localhost:8000.

## Play the games
Both are desktop Java games and need JDK 11 to 25. They don't run in the browser.

```
cd games/11-1 && ./run.sh
cd games/lindefense && ./setup.sh && ./run.sh
```
