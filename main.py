import os

import asyncpg
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

DATABASE_URL = os.getenv("DATABASE_URL")
MAX_BATCH_ITEMS = 500

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
    query = (
        "SELECT card_id, variant, card_name, market_price, low_price, mid_price, "
        "high_price, image_url, set_name, card_number, updated_at FROM latest_pokemon_prices"
    )
    args = []

    # Split into tokens so "blastoise #2" or "phantom forces 117" matches a
    # card whose name/set/number satisfy each token individually, rather than
    # requiring the whole phrase to appear in a single column.
    tokens = search.split() if search else []
    if tokens:
        clauses = []
        for token in tokens:
            token = token.lstrip("#")
            if not token:
                continue
            args.append(f"%{token}%")
            like_idx = len(args)
            if token.isdigit():
                # A bare number almost always means "card number" — match it
                # exactly there, but still allow it to appear in the name/set
                # (e.g. a card literally named "150").
                args.append(token)
                num_idx = len(args)
                clauses.append(
                    f"(card_number = ${num_idx} OR card_name ILIKE ${like_idx} OR set_name ILIKE ${like_idx})"
                )
            else:
                clauses.append(
                    f"(card_name ILIKE ${like_idx} OR set_name ILIKE ${like_idx} OR card_number ILIKE ${like_idx})"
                )
        if clauses:
            query += " WHERE " + " AND ".join(clauses)

    query += " ORDER BY card_name LIMIT $%d" % (len(args) + 1)
    args.append(limit)

    async with pool.acquire() as conn:
        rows = await conn.fetch(query, *args)
    return [dict(r) for r in rows]


class CardVariantKey(BaseModel):
    card_id: str
    variant: str


class BatchRequest(BaseModel):
    items: list[CardVariantKey]


@app.post("/cards/batch")
async def batch_prices(req: BatchRequest):
    if not req.items:
        return []
    if len(req.items) > MAX_BATCH_ITEMS:
        raise HTTPException(status_code=400, detail=f"Too many items, max {MAX_BATCH_ITEMS}")

    card_ids = [i.card_id for i in req.items]
    variants = [i.variant for i in req.items]
    query = """
        SELECT card_id, variant, card_name, market_price, low_price, mid_price,
               high_price, image_url, set_name, card_number, updated_at
        FROM latest_pokemon_prices
        WHERE (card_id, variant) IN (
            SELECT * FROM unnest($1::text[], $2::text[])
        )
    """
    async with pool.acquire() as conn:
        rows = await conn.fetch(query, card_ids, variants)
    return [dict(r) for r in rows]


@app.get("/cards/{card_id}")
async def get_card(card_id: str):
    query = """
        SELECT card_id, variant, card_name, market_price, low_price, mid_price,
               high_price, image_url, set_name, card_number, updated_at
        FROM latest_pokemon_prices WHERE card_id = $1
        ORDER BY variant
    """
    async with pool.acquire() as conn:
        rows = await conn.fetch(query, card_id)
    if not rows:
        raise HTTPException(status_code=404, detail="Card not found")
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
