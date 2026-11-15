import { test } from "node:test";
import assert from "node:assert/strict";
import {
  cupcakeImage,
  changeQuantity,
  normalizeCart,
  reconcileCart,
  totalCents,
  orderPayload,
  readStorage,
  request,
  escapeHtml,
} from "../../main/resources/static/store.mjs";

test("cada sabor usa a imagem gerada, mesmo com URL antiga no banco", () => {
  for (const [sabor, file] of [
    ["Chocolate", "chocolate"],
    ["Morango", "morango"],
    ["Baunilha", "baunilha"],
    ["Limão", "limao"],
    ["Red Velvet", "red-velvet"],
    ["Cenoura", "cenoura"],
    ["Coco", "coco"],
    ["Doce de Leite", "doce-de-leite"],
  ]) {
    assert.equal(
      cupcakeImage({ sabor, imagemUrl: "https://exemplo.com/foto.jpg" }),
      `/images/cupcakes/${file}.png`,
    );
  }
  assert.equal(
    cupcakeImage({ sabor: "Limão siciliano" }),
    "/images/cupcakes/limao.png",
  );
  assert.equal(
    cupcakeImage({ nome: "Cupcake de Chocolate", sabor: "Especial" }),
    "/images/cupcakes/chocolate.png",
  );
  assert.equal(
    cupcakeImage({ sabor: "Pistache", imagemUrl: "javascript:alert(1)" }),
    "/images/cupcakes/baunilha.png",
  );
});
test("sacola agrupa itens, respeita limite e remove quantidade zero", () => {
  let cart = changeQuantity([], 1, 1);
  cart = changeQuantity(cart, 1, 1);
  assert.deepEqual(cart, [{ cupcakeId: 1, quantidade: 2 }]);
  assert.deepEqual(changeQuantity(cart, 1, 200), [
    { cupcakeId: 1, quantidade: 100 },
  ]);
  assert.deepEqual(changeQuantity(cart, 1, -2), []);
  assert.deepEqual(
    normalizeCart([
      { cupcakeId: -1, quantidade: 1 },
      { cupcakeId: 1, quantidade: 101 },
      null,
    ]),
    [],
  );
});
test("reconcilia indisponíveis e totaliza em centavos", () => {
  const cart = [
    { cupcakeId: 1, quantidade: 3 },
    { cupcakeId: 2, quantidade: 1 },
  ];
  const products = [
    { id: 1, preco: 12.3, disponivel: true },
    { id: 2, preco: 9.99, disponivel: false },
  ];
  assert.deepEqual(reconcileCart(cart, products), [cart[0]]);
  assert.equal(totalCents(cart, products), 4689);
});
test("payload inclui apenas contrato da API, sem preço fornecido pelo navegador", () => {
  assert.deepEqual(
    orderPayload(7, [{ cupcakeId: 2, quantidade: 3, preco: 0 }], {
      logradouro: " Rua A ",
      numero: "10",
      bairro: "Centro",
      cidade: "Salvador",
      estado: "ba",
      cep: "40000-000",
      nome: "Ana",
    }),
    {
      clienteId: 7,
      itens: [{ cupcakeId: 2, quantidade: 3 }],
      endereco: {
        logradouro: "Rua A",
        numero: "10",
        complemento: "",
        bairro: "Centro",
        cidade: "Salvador",
        estado: "BA",
        cep: "40000-000",
      },
    },
  );
});
test("storage inválido e conteúdo HTML não quebram a interface", () => {
  assert.deepEqual(readStorage({ getItem: () => "{invalido" }, "cart", []), []);
  assert.equal(escapeHtml('<script>"&'), "&lt;script&gt;&quot;&amp;");
});
test("request envia JSON e apresenta erros da API e de conexão", async () => {
  let sent;
  await request("/api/pedidos", {
    method: "POST",
    body: { clienteId: 1 },
    fetchImpl: async (path, options) => {
      sent = { path, ...options };
      return { ok: true, json: async () => ({ id: 4 }) };
    },
  });
  assert.equal(sent.method, "POST");
  assert.equal(sent.body, '{"clienteId":1}');
  await assert.rejects(
    request("/api/pedidos", {
      fetchImpl: async () => ({
        ok: false,
        status: 409,
        json: async () => ({ detail: "Cupcake indisponivel." }),
      }),
    }),
    /Cupcake indisponivel/,
  );
  await assert.rejects(
    request("/api/cupcakes", {
      fetchImpl: async () => {
        throw Error("offline");
      },
    }),
    /conectar/,
  );
});
