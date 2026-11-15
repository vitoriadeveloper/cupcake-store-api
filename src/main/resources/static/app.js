import {
  money,
  escapeHtml as e,
  readStorage,
  normalizeCart,
  changeQuantity,
  reconcileCart,
  totalCents,
  orderPayload,
  request,
  cupcakeImage as image,
} from "./store.mjs";
const $ = (selector) => document.querySelector(selector);
const storage = {
  getItem: (key) => {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  },
  setItem: (key, value) => {
    try {
      localStorage.setItem(key, value);
    } catch {}
  },
};
let cart = normalizeCart(readStorage(storage, "doce.cart", []));
let customer = readStorage(storage, "doce.customer", null);
if (!Number.isSafeInteger(customer?.id) || customer.id < 1) customer = null;
let products = [],
  flavor = "",
  search = "",
  busy = false,
  catalogReady = false;
let toastTimer;
function toast(message) {
  $("#toast").textContent = message;
  $("#toast").hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => ($("#toast").hidden = true), 4000);
}
function saveCart() {
  storage.setItem("doce.cart", JSON.stringify(cart));
  renderCart();
}
function saveCustomer(value) {
  customer = value;
  storage.setItem("doce.customer", JSON.stringify(value));
}
function open(id) {
  document.querySelectorAll("dialog[open]").forEach((d) => d.close());
  $(id).showModal();
}
document
  .querySelectorAll("[data-close]")
  .forEach((button) =>
    button.addEventListener("click", () => button.closest("dialog").close()),
  );
document.querySelectorAll("dialog").forEach((dialog) =>
  dialog.addEventListener("click", (event) => {
    if (event.target === dialog) {
      const r = dialog.getBoundingClientRect();
      if (
        event.clientX < r.left ||
        event.clientX > r.right ||
        event.clientY < r.top ||
        event.clientY > r.bottom
      )
        dialog.close();
    }
  }),
);
function renderProducts() {
  const visible = products.filter(
    (p) =>
      (!flavor || p.sabor === flavor) &&
      `${p.nome} ${p.sabor} ${p.descricao}`
        .toLocaleLowerCase("pt-BR")
        .includes(search),
  );
  $("#products").innerHTML = visible
    .map(
      (p) =>
        `<article class="product"><div class="product-image"><img src="${e(image(p))}" alt="${e(p.nome)}" loading="lazy"><span class="tag">${e(p.sabor)}</span></div><div class="product-body"><h3>${e(p.nome)}</h3><p>${e(p.descricao)}</p><div class="product-bottom"><span class="price">${money(p.preco)}</span><button class="add" data-add="${p.id}" ${!p.disponivel ? "disabled" : ""}>${p.disponivel ? "Adicionar +" : "Indisponível"}</button></div></div></article>`,
    )
    .join("");
  $("#catalog-status").innerHTML = !visible.length
    ? '<p class="empty">' +
      (products.length
        ? "Nenhum cupcake encontrado. Experimente outro sabor."
        : "O forno está aquecendo! Nosso cardápio estará disponível em breve.") +
      "</p>"
    : "";
  $("#products")
    .querySelectorAll("img")
    .forEach((img) =>
      img.addEventListener(
        "error",
        () => {
          img.src = "/images/cupcakes/baunilha.png";
        },
        { once: true },
      ),
    );
}
async function loadCatalog() {
  $("#catalog-status").innerHTML =
    '<p class="loading">Preparando o cardápio…</p>';
  try {
    products = await request("/api/cupcakes");
    catalogReady = true;
    const current = JSON.stringify(cart);
    cart = reconcileCart(cart, products);
    saveCart();
    if (current !== JSON.stringify(cart))
      toast(
        "A sacola foi atualizada: alguns produtos não estão mais disponíveis.",
      );
    $("#filters").innerHTML = ["", ...new Set(products.map((p) => p.sabor))]
      .map(
        (s) =>
          `<button class="filter ${s === flavor ? "active" : ""}" data-flavor="${e(s)}" aria-pressed="${s === flavor}">${e(s || "Todos os sabores")}</button>`,
      )
      .join("");
    renderProducts();
  } catch (error) {
    catalogReady = false;
    $("#catalog-status").innerHTML =
      `<p class="error">${e(error.message)}</p><button class="text-button" id="retry-catalog">Tentar novamente</button>`;
    $("#retry-catalog").onclick = loadCatalog;
    renderCart();
  }
}
$("#search").addEventListener("input", (event) => {
  search = event.target.value.trim().toLocaleLowerCase("pt-BR");
  renderProducts();
});
$("#filters").addEventListener("click", (event) => {
  const button = event.target.closest("[data-flavor]");
  if (!button) return;
  flavor = button.dataset.flavor;
  $("#filters")
    .querySelectorAll("button")
    .forEach((b) => {
      b.classList.toggle("active", b === button);
      b.setAttribute("aria-pressed", String(b === button));
    });
  renderProducts();
});
$("#products").addEventListener("click", (event) => {
  const button = event.target.closest("[data-add]");
  if (!button) return;
  const id = Number(button.dataset.add);
  if (cart.find((i) => i.cupcakeId === id)?.quantidade === 100) {
    toast("O limite é de 100 unidades por produto.");
    return;
  }
  cart = changeQuantity(cart, id, 1);
  saveCart();
  toast("Cupcake adicionado à sacola.");
});
function renderCart() {
  $("#cart-count").textContent = cart.reduce((sum, i) => sum + i.quantidade, 0);
  $("#cart-items").innerHTML = cart.length
    ? cart
        .map((i) => {
          const p = products.find((p) => p.id === i.cupcakeId);
          if (!p) return "";
          return `<div class="cart-row"><img src="${e(image(p))}" alt=""><div class="cart-info"><h3>${e(p.nome)}</h3><small>${money(p.preco)} cada</small><br><button class="remove" data-remove="${p.id}">Remover</button></div><div class="quantity"><button data-quantity="${p.id}" data-delta="-1" aria-label="Diminuir quantidade de ${e(p.nome)}">−</button><span>${i.quantidade}</span><button data-quantity="${p.id}" data-delta="1" ${i.quantidade >= 100 ? "disabled" : ""} aria-label="Aumentar quantidade de ${e(p.nome)}">+</button></div></div>`;
        })
        .join("")
    : '<p class="empty">Sua sacola está esperando um pouco de doce.<br>Escolha um cupcake no cardápio.</p>';
  $("#cart-total").textContent = money(totalCents(cart, products) / 100);
  $("#checkout-button").disabled = !cart.length || !catalogReady;
}
$("#cart-button").onclick = () => {
  renderCart();
  open("#cart-dialog");
};
$("#cart-items").onclick = (event) => {
  const remove = event.target.closest("[data-remove]"),
    quantity = event.target.closest("[data-quantity]");
  if (remove)
    cart = cart.filter((i) => i.cupcakeId !== Number(remove.dataset.remove));
  else if (quantity)
    cart = changeQuantity(
      cart,
      Number(quantity.dataset.quantity),
      Number(quantity.dataset.delta),
    );
  else return;
  saveCart();
};
for (const uf of "AC AL AP AM BA CE DF ES GO MA MT MS MG PA PB PR PE PI RJ RN RS RO RR SC SP SE TO".split(
  " ",
))
  $("#checkout-form select").add(new Option(uf, uf));
$("#checkout-button").onclick = () => {
  if (!cart.length) return;
  $("#checkout-error").textContent = "";
  if (customer)
    for (const key of ["nome", "email", "telefone"])
      $("#checkout-form").elements[key].value = customer[key] || "";
  open("#checkout-dialog");
};
$("#checkout-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  if (busy || !cart.length) return;
  busy = true;
  $("#submit-order").disabled = true;
  $("#submit-order").textContent = "Salvando pedido…";
  $("#checkout-error").textContent = "";
  try {
    const fields = Object.fromEntries(new FormData(event.target));
    const profile = {
      nome: fields.nome.trim(),
      email: fields.email.trim().toLowerCase(),
      telefone: fields.telefone.trim(),
    };
    // Reuse only the customer already identified on this browser; never download the customer list.
    const sameCustomer = customer && customer.email === profile.email;
    const saved = await request(
      sameCustomer ? `/api/clientes/${customer.id}` : "/api/clientes",
      { method: sameCustomer ? "PUT" : "POST", body: profile },
    );
    saveCustomer(saved);
    const order = await request("/api/pedidos", {
      method: "POST",
      body: orderPayload(saved.id, cart, fields),
    });
    cart = [];
    saveCart();
    $("#checkout-dialog").close();
    toast(`Pedido #${order.id} realizado! Total: ${money(order.valorTotal)}.`);
    open("#history-dialog");
    await loadOrders();
  } catch (error) {
    $("#checkout-error").textContent =
      error.message +
      (error.status === 409
        ? " Se já possui cadastro, use os dados do cadastro deste navegador."
        : "");
  } finally {
    busy = false;
    $("#submit-order").disabled = false;
    $("#submit-order").textContent = "Confirmar pedido";
  }
});
const statusNames = {
  AGUARDANDO_PAGAMENTO: "Aguardando pagamento",
  PEDIDO_ENVIADO: "Pedido enviado",
  PREPARANDO: "Em preparo",
  PRONTO: "Pronto para entrega",
  ENTREGUE: "Entregue",
  CANCELADO: "Cancelado",
};
async function loadOrders() {
  if (!customer) {
    $("#orders").innerHTML =
      '<p class="empty">Você ainda não realizou pedidos neste navegador.<br>Seu primeiro pedido começa no cardápio.</p>';
    return;
  }
  $("#orders").innerHTML = '<p class="loading">Buscando seus pedidos…</p>';
  try {
    const orders = await request(`/api/clientes/${customer.id}/pedidos`);
    $("#orders").innerHTML = orders.length
      ? orders
          .map(
            (o) =>
              `<article class="order"><div class="order-head"><h3>Pedido #${o.id}</h3><span class="status">${e(statusNames[o.status] || o.status)}</span></div><p class="muted">${e(new Date(o.dataCriacao).toLocaleString("pt-BR"))}</p><ul>${o.itens.map((i) => `<li>${i.quantidade} × ${e(i.nome)} · ${money(i.precoUnitario * i.quantidade)}</li>`).join("")}</ul><p class="order-address">${e(o.endereco.logradouro)}, ${e(o.endereco.numero)}${o.endereco.complemento ? " · " + e(o.endereco.complemento) : ""}<br>${e(o.endereco.bairro)} · ${e(o.endereco.cidade)}/${e(o.endereco.estado)} · ${e(o.endereco.cep)}</p><p class="order-total">Total ${money(o.valorTotal)} · Entrega grátis</p>${o.status === "AGUARDANDO_PAGAMENTO" ? '<p class="muted">Pedido registrado. O pagamento de demonstração funciona apenas no perfil local, sem cobrança real.</p>' : ""}<div class="order-actions">${o.status === "AGUARDANDO_PAGAMENTO" ? `<button class="text-button" data-pay="${o.id}" data-method="CREDITO">Simular crédito</button><button class="text-button" data-pay="${o.id}" data-method="DEBITO">Simular débito</button>` : ""}${["AGUARDANDO_PAGAMENTO", "PEDIDO_ENVIADO"].includes(o.status) ? `<button class="text-button" data-cancel="${o.id}">Cancelar pedido</button>` : ""}</div></article>`,
          )
          .join("")
      : '<p class="empty">Você ainda não tem pedidos.</p>';
  } catch (error) {
    $("#orders").innerHTML = `<p class="error">${e(error.message)}</p>`;
  }
}
$("#history-button").onclick = () => {
  open("#history-dialog");
  loadOrders();
};
$("#refresh-orders").onclick = loadOrders;
$("#orders").onclick = async (event) => {
  const pay = event.target.closest("[data-pay]"),
    cancel = event.target.closest("[data-cancel]");
  if ((!pay && !cancel) || busy) return;
  if (cancel && !window.confirm(`Cancelar o pedido #${cancel.dataset.cancel}?`))
    return;
  busy = true;
  const button = pay || cancel;
  button.disabled = true;
  try {
    if (pay)
      await request(`/api/pedidos/${pay.dataset.pay}/pagamento-simulado`, {
        method: "POST",
        body: { formaPagamento: pay.dataset.method },
      });
    else
      await request(`/api/pedidos/${cancel.dataset.cancel}/cancelamento`, {
        method: "PATCH",
      });
    toast(
      pay
        ? "Pagamento simulado aprovado. Nenhuma cobrança realizada."
        : "Pedido cancelado.",
    );
    await loadOrders();
  } catch (error) {
    toast(
      pay && error.status === 404
        ? "Simulação indisponível. Inicie o servidor com o perfil local para simular pagamentos."
        : error.message,
    );
    button.disabled = false;
  } finally {
    busy = false;
  }
};
renderCart();
loadCatalog();
