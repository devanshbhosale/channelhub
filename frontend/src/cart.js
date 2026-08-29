import { useSyncExternalStore } from 'react';

const KEY = 'ch_cart';
const listeners = new Set();

function read() {
  try {
    const v = JSON.parse(localStorage.getItem(KEY) || '[]');
    return Array.isArray(v) ? v : [];
  } catch {
    return [];
  }
}

let current = read();

export function getCart() {
  return current;
}

export function setCart(items) {
  current = Array.isArray(items) ? items : [];
  localStorage.setItem(KEY, JSON.stringify(current));
  listeners.forEach((l) => l());
}

export function addToCart(entry) {
  const next = current.slice();
  const idx = next.findIndex((i) => i.productId === entry.productId);
  if (idx >= 0) next[idx] = { ...next[idx], qty: next[idx].qty + entry.qty };
  else next.push(entry);
  setCart(next);
}

export function updateQty(productId, qty) {
  const q = Math.max(1, Math.floor(Number(qty)) || 1);
  setCart(current.map((i) => (i.productId === productId ? { ...i, qty: q } : i)));
}

export function removeFromCart(productId) {
  setCart(current.filter((i) => i.productId !== productId));
}

export function clearCart() {
  setCart([]);
}

export function cartTotal(items) {
  return (items || current).reduce((s, i) => s + Number(i.unitPrice) * Number(i.qty), 0);
}

export function useCart() {
  return useSyncExternalStore(
    (cb) => {
      listeners.add(cb);
      return () => listeners.delete(cb);
    },
    getCart,
    getCart
  );
}
