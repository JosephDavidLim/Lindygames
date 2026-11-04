// Numbers come from EnemyType and text from DefenderType in games/11-1/src/entities.
const CONSTRUCTS = [
  { id: "homework_sheet", name: "Homework Sheet", hp: 100, bite: 10, fear: "Fear of never being finished." },
  { id: "sharp_pencil", name: "Sharp Pencil", hp: 60, bite: 8, fear: "Fear of the blank page, and of what you'll put on it." },
  { id: "report_card", name: "Report Card", hp: 250, bite: 10, fear: "Fear of disappointing the people who love you." },
  { id: "paper_airplane", name: "Paper Airplane", hp: 120, bite: 10, fear: "Fear of being caught not paying attention." },
  { id: "eraser", name: "Eraser", hp: 150, bite: 30, fear: "Fear that all your work could simply disappear." },
  { id: "scantron_swarm", name: "Scantron Swarm", hp: 60, bite: 8, fear: "Fear of being reduced to a number." },
  { id: "application_fee", name: "Application Fee", hp: 120, bite: 10, fear: "Fear that your future costs more than you have." },
  { id: "personal_essay", name: "Personal Essay", hp: 400, bite: 15, fear: "Fear that you have nothing worth saying." },
  { id: "rejection_letter", name: "Rejection Letter", hp: 250, bite: 45, fear: "Fear of not being enough." },
  { id: "the_deadline", name: "The Deadline", hp: 3000, bite: 40, boss: true, fear: "Fear of running out of time. Everyone's." }
];

const PEOPLE = [
  ["luis", "Lobber Luis", "Rail Arm: throws hard and straight"],
  ["aarav", "Sage Aarav", "Focus Miner: turns attention into currency"],
  ["imraan", "Iron Imraan", "Plate Spine: rebuilt in steel, repairs itself"],
  ["sam", "Boom Sam", "Chest Charge: a demolition scholarship"],
  ["austin", "Chillstin", "Cryo Blood: chills what it touches, and its owner"],
  ["kai", "Kaiju Kai", "Quake Legs: stomps everything nearby"],
  ["abhinav", "Abhi-Ace", "Vector Arm: shots find a second target"],
  ["matt", "Zippy Matt", "Spread Rig: fires down three lanes"],
  ["joseph", "Sunny Joe", "Medic Array: heals others automatically"],
  ["nik", "Nik-Scope", "Target Lens: reads anyone's grade from a mile away"],
  ["camilo", "Cami-Mine", "Mine Hands: good at waiting"],
  ["justin", "Justin Time", "Close Coil: works only when it's nearly too late"],
  ["hanbo", "Spear Hanbo", "Lance Frame: drives through a whole line"],
  ["tuhin", "Thorn Tuhin", "Thorn Skin: nothing can hold on"],
  ["james", "Trapper Che", "Snare Rig: stops whoever steps on it"],
  ["arnav", "Blitz Arnav", "One Shot: built to be used once"],
  ["tanish", "Tanimbus", "Storm Coil: mines Focus and calls lightning"]
];

function makeImage(src, alt) {
  const img = new Image();
  img.src = src;
  img.alt = alt;
  img.width = 128;
  img.height = 128;
  img.loading = "lazy";
  return img;
}

function setupConstructs() {
  const grid = document.getElementById("construct-grid");
  const readout = document.getElementById("construct-readout");
  const maxHp = 3000;
  const maxBite = 45;

  function show(index) {
    const c = CONSTRUCTS[index];
    [...grid.children].forEach((b, i) => b.setAttribute("aria-pressed", String(i === index)));
    readout.replaceChildren();
    readout.append(makeImage(`assets/constructs/${c.id}.png`, ""));
    const h = document.createElement("h3");
    h.textContent = c.name;
    const born = document.createElement("p");
    born.className = "born";
    born.textContent = c.boss ? "Boss construct. Born from" : "Born from";
    const fear = document.createElement("p");
    fear.className = "fear";
    fear.textContent = c.fear;
    readout.append(h, born, fear, meter("Mass", c.hp, Math.sqrt(c.hp / maxHp), ""), meter("Bite", c.bite, c.bite / maxBite, "bite"));
  }

  function meter(label, value, fraction, kind) {
    const row = document.createElement("div");
    row.className = `meter ${kind}`;
    const name = document.createElement("span");
    name.textContent = label;
    const track = document.createElement("div");
    const fill = document.createElement("i");
    fill.style.width = `${Math.round(fraction * 100)}%`;
    track.append(fill);
    const number = document.createElement("span");
    number.textContent = value;
    row.append(name, track, number);
    return row;
  }

  CONSTRUCTS.forEach((c, i) => {
    const b = document.createElement("button");
    b.type = "button";
    b.className = "tile";
    b.append(makeImage(`assets/constructs/${c.id}.png`, ""), c.name);
    b.addEventListener("click", () => show(i));
    grid.append(b);
  });
  show(CONSTRUCTS.length - 1);
}

function setupPeople() {
  const roster = document.getElementById("roster");
  const line = document.getElementById("person-line");

  function show(index) {
    const [, name, construct] = PEOPLE[index];
    [...roster.children].forEach((b, i) => b.setAttribute("aria-pressed", String(i === index)));
    const parts = construct.split(": ");
    line.replaceChildren();
    const who = document.createElement("b");
    who.textContent = name;
    const what = document.createElement("span");
    what.textContent = `${parts[0]}. ${parts[1].charAt(0).toUpperCase()}${parts[1].slice(1)}.`;
    line.append(who, what);
  }

  PEOPLE.forEach(([id, name], i) => {
    const b = document.createElement("button");
    b.type = "button";
    b.className = "person";
    b.append(makeImage(`assets/people/${id}.png`, ""), name);
    b.addEventListener("click", () => show(i));
    roster.append(b);
  });
  show(0);
}

function setupTabs() {
  const tabs = [...document.querySelectorAll('[role="tab"]')];

  function select(tab) {
    tabs.forEach((t) => {
      const on = t === tab;
      t.setAttribute("aria-selected", String(on));
      t.tabIndex = on ? 0 : -1;
      document.getElementById(t.getAttribute("aria-controls")).hidden = !on;
    });
    tab.focus({ preventScroll: true });
  }

  tabs.forEach((tab, i) => {
    tab.addEventListener("click", () => select(tab));
    tab.addEventListener("keydown", (e) => {
      if (e.key === "ArrowRight" || e.key === "ArrowLeft") {
        const step = e.key === "ArrowRight" ? 1 : tabs.length - 1;
        select(tabs[(i + step) % tabs.length]);
      }
    });
  });
}

function setupCopy() {
  document.querySelectorAll("[data-copy]").forEach((b) => {
    b.addEventListener("click", async () => {
      try {
        await navigator.clipboard.writeText(b.dataset.copy);
        b.textContent = "Copied";
      } catch (e) {
        b.textContent = "Select it";
      }
      setTimeout(() => { b.textContent = "Copy"; }, 1600);
    });
  });
}

function setupRain() {
  const canvas = document.getElementById("rain");
  if (matchMedia("(prefers-reduced-motion: reduce)").matches) {
    return;
  }
  const ctx = canvas.getContext("2d");
  let drops = [];

  function resize() {
    canvas.width = innerWidth;
    canvas.height = innerHeight;
    const count = Math.min(140, Math.floor(innerWidth / 11));
    drops = Array.from({ length: count }, () => ({
      x: Math.random() * innerWidth,
      y: Math.random() * innerHeight,
      speed: 7 + Math.random() * 8,
      length: 8 + Math.random() * 12
    }));
  }

  function frame() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.strokeStyle = "rgba(160, 205, 255, .16)";
    ctx.lineWidth = 1;
    ctx.beginPath();
    for (const d of drops) {
      ctx.moveTo(d.x, d.y);
      ctx.lineTo(d.x - 1.5, d.y + d.length);
      d.y += d.speed;
      d.x -= 0.5;
      if (d.y > canvas.height) {
        d.y = -d.length;
        d.x = Math.random() * canvas.width;
      }
    }
    ctx.stroke();
    requestAnimationFrame(frame);
  }

  addEventListener("resize", resize);
  resize();
  frame();
}

setupConstructs();
setupPeople();
setupTabs();
setupCopy();
setupRain();
