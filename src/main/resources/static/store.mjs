export const money = (value) =>
  new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(
    value,
  );
export function cupcakeImage(product) {
  const normalize = (value) =>
    String(value ?? "")
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, " ")
      .trim();
  const flavors = [
    "doce de leite",
    "red velvet",
    "chocolate",
    "morango",
    "baunilha",
    "limao",
    "cenoura",
    "coco",
  ];
  const flavor = normalize(product.sabor),
    name = normalize(product.nome);
  const match =
    flavors.find((f) => flavor === f) ||
    flavors.find((f) => new RegExp(`\\b${f}\\b`).test(flavor)) ||
    flavors.find((f) => new RegExp(`\\b${f}\\b`).test(name));
  if (match) return `/images/cupcakes/${match.replaceAll(" ", "-")}.png`;
  const url = product.imagemUrl;
  if (
    url &&
    ((url.startsWith("/") && !url.startsWith("//")) ||
      /^https?:\/\//i.test(url))
  )
    return url;
  return "/images/cupcakes/baunilha.png";
}
export const escapeHtml = (value) =>
  String(value ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
export function readStorage(storage, key, fallback) {
  try {
    return JSON.parse(storage.getItem(key)) ?? fallback;
  } catch {
    return fallback;
  }
}
export function normalizeCart(cart) {
  if (!Array.isArray(cart)) return [];
  const seen = new Set();
  return cart
    .filter(
      (i) =>
        i &&
        Number.isSafeInteger(i.cupcakeId) &&
        i.cupcakeId > 0 &&
        Number.isInteger(i.quantidade) &&
        i.quantidade > 0 &&
        i.quantidade <= 100 &&
        !seen.has(i.cupcakeId) &&
        seen.add(i.cupcakeId),
    )
    .slice(0, 100)
    .map(({ cupcakeId, quantidade }) => ({ cupcakeId, quantidade }));
}
export function changeQuantity(cart, id, delta) {
  const next = normalizeCart(cart).map((i) => ({ ...i }));
  const item = next.find((i) => i.cupcakeId === id);
  if (item) item.quantidade = Math.min(100, item.quantidade + delta);
  else if (delta > 0 && next.length < 100)
    next.push({ cupcakeId: id, quantidade: Math.min(100, delta) });
  return normalizeCart(next);
}
export function reconcileCart(cart, products) {
  return normalizeCart(cart).filter((i) =>
    products.some((p) => p.id === i.cupcakeId && p.disponivel),
  );
}
export function totalCents(cart, products) {
  return cart.reduce(
    (sum, i) =>
      sum +
      Math.round(
        Number(products.find((p) => p.id === i.cupcakeId)?.preco ?? 0) * 100,
      ) *
        i.quantidade,
    0,
  );
}
export function orderPayload(clienteId, cart, fields) {
  const endereco = {};
  for (const key of [
    "logradouro",
    "numero",
    "complemento",
    "bairro",
    "cidade",
    "estado",
    "cep",
  ])
    endereco[key] = String(fields[key] ?? "").trim();
  endereco.estado = endereco.estado.toUpperCase();
  return { clienteId, itens: normalizeCart(cart), endereco };
}
export async function request(
  path,
  { method = "GET", body, fetchImpl = globalThis.fetch } = {},
) {
  let response;
  try {
    response = await fetchImpl(path, {
      method,
      headers: body ? { "Content-Type": "application/json" } : {},
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new Error(
      "Não foi possível conectar à loja. Confira sua conexão e tente novamente.",
    );
  }
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const fields = data.campos?.map((f) => f.campo).join(", ");
    const error = new Error(
      (data.detail || "Não foi possível concluir esta operação.") +
        (fields ? ` Verifique: ${fields}.` : ""),
    );
    error.status = response.status;
    throw error;
  }
  return data;
}
