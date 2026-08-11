import os

import asyncpg
from fastapi import FastAPI, HTTPException

DATABASE_URL = os.getenv("DATABASE_URL")

app = FastAPI(title="PokeApp API")
pool: asyncpg.Pool | None = None


@app.on_event("startup")
async def startup():
    global pool
    if not DATABASE_URL:
        raise ValueError("DATABASE_URL environment variable is missing!")
    pool = await asyncpg.create_pool(DATABASE_URL, min_size=1, max_size=5)


@app.on_event("shutdown")
async def shutdown():
    if pool:
        await pool.close()


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.get("/cards")
async def list_cards(search: str | None = None, limit: int = 50):
    query = "SELECT card_id, variant, card_name, market_price, low_price, mid_price, high_price, updated_at FROM latest_pokemon_prices"
    args = []
    if search:
        query += " WHERE card_name ILIKE $1"
        args.append(f"%{search}%")
    query += " ORDER BY card_name LIMIT $%d" % (len(args) + 1)
    args.append(limit)

    async with pool.acquire() as conn:
        rows = await conn.fetch(query, *args)
    return [dict(r) for r in rows]


@app.get("/cards/{card_id}/history")
async def card_history(card_id: str, variant: str | None = None):
    query = "SELECT card_id, card_name, variant, market_price, low_price, mid_price, high_price, fetched_at FROM pokemon_price_history WHERE card_id = $1"
    args = [card_id]
    if variant:
        query += " AND variant = $2"
        args.append(variant)
    query += " ORDER BY fetched_at ASC"

    async with pool.acquire() as conn:
        rows = await conn.fetch(query, *args)
    if not rows:
        raise HTTPException(status_code=404, detail="No history found for this card")
    return [dict(r) for r in rows]
