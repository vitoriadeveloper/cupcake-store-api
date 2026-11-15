import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
const base = "http://127.0.0.1:18080";
const profile = await mkdtemp(join(tmpdir(), "doce-browser-"));
const chrome =
  process.env.CHROME_PATH ||
  "C:/Program Files/Google/Chrome/Application/chrome.exe";
const child = spawn(
  chrome,
  [
    "--headless=new",
    "--disable-gpu",
    "--no-first-run",
    "--no-default-browser-check",
    "--remote-debugging-port=19222",
    `--user-data-dir=${profile}`,
    "about:blank",
  ],
  { windowsHide: true, stdio: "ignore" },
);
let socket;
try {
  const post = async (path, body) => {
    const r = await fetch(base + path, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    assert.ok(r.ok, await r.clone().text());
    return r.json();
  };
  const product = await post("/api/cupcakes", {
    nome: "Cupcake de Limão",
    descricao: "Massa leve com cobertura cítrica.",
    preco: 12.5,
    sabor: "Limão",
    imagemUrl: "https://exemplo.com/antiga.jpg",
    disponivel: true,
  });
  await post("/api/cupcakes", {
    nome: "Morango indisponível",
    descricao: "Morango fresco.",
    preco: 15,
    sabor: "Morango",
    disponivel: false,
  });
  let tabs;
  for (let i = 0; i < 100; i++) {
    try {
      tabs = await (await fetch("http://127.0.0.1:19222/json")).json();
      break;
    } catch {
      await new Promise((r) => setTimeout(r, 100));
    }
  }
  assert.ok(tabs, "Chrome deve iniciar");
  socket = new WebSocket(tabs[0].webSocketDebuggerUrl);
  await new Promise((resolve, reject) => {
    socket.onopen = resolve;
    socket.onerror = reject;
  });
  let seq = 0;
  const pending = new Map();
  const errors = [];
  socket.onmessage = (event) => {
    const message = JSON.parse(event.data);
    if (message.id) {
      const item = pending.get(message.id);
      pending.delete(message.id);
      message.error
        ? item.reject(Error(message.error.message))
        : item.resolve(message.result);
    }
    if (message.method === "Runtime.exceptionThrown")
      errors.push(message.params.exceptionDetails.text);
  };
  const cdp = (method, params = {}) =>
    new Promise((resolve, reject) => {
      const id = ++seq;
      pending.set(id, { resolve, reject });
      socket.send(JSON.stringify({ id, method, params }));
    });
  const evaluate = async (expression) => {
    const r = await cdp("Runtime.evaluate", {
      expression,
      returnByValue: true,
      awaitPromise: true,
    });
    if (r.exceptionDetails) throw Error(JSON.stringify(r.exceptionDetails));
    return r.result.value;
  };
  const wait = async (expression) => {
    for (let i = 0; i < 100; i++) {
      if (await evaluate(expression)) return;
      await new Promise((r) => setTimeout(r, 100));
    }
    throw Error("Timeout: " + expression);
  };
  const click = (selector) =>
    evaluate(`document.querySelector(${JSON.stringify(selector)}).click()`);
  await cdp("Runtime.enable");
  await cdp("Page.enable");
  await cdp("Page.navigate", { url: base });
  await wait('document.querySelectorAll(".product").length >= 2');
  assert.equal(
    await evaluate(
      'document.querySelector(".product img").getAttribute("src")',
    ),
    "/images/cupcakes/limao.png",
  );
  await wait(
    '[...document.querySelectorAll(".product img")].every(i=>i.complete&&i.naturalWidth>0)',
  );
  assert.equal(
    await evaluate('document.querySelectorAll(".add:disabled").length'),
    1,
  );
  await click(`[data-add="${product.id}"]`);
  await click(`[data-add="${product.id}"]`);
  assert.equal(
    await evaluate('document.querySelector("#cart-count").textContent'),
    "2",
  );
  await cdp("Page.reload");
  await wait('document.querySelectorAll(".product").length >= 2');
  assert.equal(
    await evaluate('document.querySelector("#cart-count").textContent'),
    "2",
  );
  await click("#cart-button");
  await click("#checkout-button");
  const email = `browser-${Date.now()}@example.com`;
  await evaluate(
    `(()=>{const f=document.querySelector('#checkout-form');const data=${JSON.stringify({ nome: "Ana Teste", email, telefone: "71999999999", cep: "40000-000", estado: "BA", logradouro: "Rua das Flores", numero: "123", bairro: "Centro", cidade: "Salvador" })};for(const [k,v] of Object.entries(data))f.elements[k].value=v;f.requestSubmit();})()`,
  );
  await wait('document.querySelector(".order") !== null');
  assert.equal(
    await evaluate('document.querySelector("#cart-count").textContent'),
    "0",
  );
  const customer = await evaluate(
    'JSON.parse(localStorage.getItem("doce.customer"))',
  );
  let orders = await (
    await fetch(`${base}/api/clientes/${customer.id}/pedidos`)
  ).json();
  assert.equal(orders[0].valorTotal, 25);
  assert.equal(orders[0].itens[0].quantidade, 2);
  assert.equal(orders[0].endereco.logradouro, "Rua das Flores");
  await click("[data-pay]");
  await wait(
    'document.querySelector(".status").textContent === "Pedido enviado"',
  );
  await evaluate("window.confirm=()=>true");
  await click("[data-cancel]");
  await wait('document.querySelector(".status").textContent === "Cancelado"');
  orders = await (
    await fetch(`${base}/api/clientes/${customer.id}/pedidos`)
  ).json();
  assert.equal(orders[0].status, "CANCELADO");
  await click("#history-dialog [data-close]");
  await cdp("Emulation.setDeviceMetricsOverride", {
    width: 390,
    height: 844,
    deviceScaleFactor: 1,
    mobile: true,
  });
  assert.equal(
    await evaluate("document.documentElement.scrollWidth <= window.innerWidth"),
    true,
    "Layout mobile sem rolagem horizontal",
  );
  await writeFile(
    "target/loja-mobile.png",
    Buffer.from(
      (await cdp("Page.captureScreenshot", { format: "png" })).data,
      "base64",
    ),
  );
  assert.deepEqual(errors, []);
  console.log(
    "PASS: imagens locais, indisponibilidade, sacola, recarga, checkout, persistência HTTP, pagamento, cancelamento e layout mobile.",
  );
  await cdp("Browser.close");
} finally {
  socket?.close();
  child.kill();
  await new Promise((r) => setTimeout(r, 500));
  await rm(profile, {
    recursive: true,
    force: true,
    maxRetries: 5,
    retryDelay: 200,
  }).catch(() => {});
}
