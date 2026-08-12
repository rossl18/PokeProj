import asyncio
import os
import aiohttp
import asyncpg

BASE_URL = "https://api.tcgdex.net/v2/en"
CONCURRENCY_LIMIT = 25
DATABASE_URL = os.getenv("DATABASE_URL")


async def fetch_card(session, card_id, semaphore):
    async with semaphore:
        try:
            async with session.get(
                f"{BASE_URL}/cards/{card_id}", timeout=10
            ) as resp:
                if resp.status == 200:
                    return await resp.json()
        except Exception:
            return None
    return None


async def main():
    if not DATABASE_URL:
        raise ValueError("DATABASE_URL environment variable is missing!")

    semaphore = asyncio.Semaphore(CONCURRENCY_LIMIT)

    async with aiohttp.ClientSession() as session:
        # 1. Fetch sets
        async with session.get(f"{BASE_URL}/sets") as res:
            sets = await res.json()

        # 2. Get all Card IDs
        card_tasks = [
            session.get(f"{BASE_URL}/sets/{s.get('id')}") for s in sets
        ]
        set_responses = await asyncio.gather(*card_tasks, return_exceptions=True)

        all_card_ids = []
        for resp in set_responses:
            if hasattr(resp, "status") and resp.status == 200:
                data = await resp.json()
                for c in data.get("cards", []):
                    all_card_ids.append(c.get("id"))

        print(f"Fetching pricing for {len(all_card_ids)} cards...")

        # 3. Fetch card payloads in parallel
        tasks = [
            fetch_card(session, cid, semaphore) for cid in all_card_ids
        ]
        results = await asyncio.gather(*tasks)

    # 4. Prepare batch records
    records = []
    for card in results:
        if not card:
            continue

        tcg_data = card.get("pricing", {}).get("tcgplayer")
        if tcg_data:
            for variant_key, prices in tcg_data.items():
                if isinstance(prices, dict) and prices.get("marketPrice"):
                    records.append((
                        card.get("id"),
                        variant_key,
                        card.get("name"),
                        prices.get("marketPrice"),
                        prices.get("lowPrice"),
                        prices.get("midPrice"),
                        prices.get("highPrice"),
                        card.get("image"),
                        card.get("set", {}).get("name"),
                        card.get("localId"),
                    ))

    print(f"Connecting to Neon to process {len(records)} records...")
    conn = await asyncpg.connect(DATABASE_URL)
    await conn.execute(
        "ALTER TABLE latest_pokemon_prices ADD COLUMN IF NOT EXISTS image_url TEXT;"
    )
    await conn.execute(
        "ALTER TABLE latest_pokemon_prices ADD COLUMN IF NOT EXISTS set_name TEXT;"
    )
    await conn.execute(
        "ALTER TABLE latest_pokemon_prices ADD COLUMN IF NOT EXISTS card_number TEXT;"
    )
    # Trigram indexes make ILIKE '%text%' search fast instead of a full table scan.
    await conn.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm;")
    await conn.execute(
        "CREATE INDEX IF NOT EXISTS idx_prices_card_name_trgm ON latest_pokemon_prices USING gin (card_name gin_trgm_ops);"
    )
    await conn.execute(
        "CREATE INDEX IF NOT EXISTS idx_prices_set_name_trgm ON latest_pokemon_prices USING gin (set_name gin_trgm_ops);"
    )

    # SQL query: Upsert into latest_pokemon_prices AND return rows where market_price actually changed
    upsert_query = """
        INSERT INTO latest_pokemon_prices
        (card_id, variant, card_name, market_price, low_price, mid_price, high_price, image_url, set_name, card_number, updated_at)
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, CURRENT_TIMESTAMP)
        ON CONFLICT (card_id, variant) DO UPDATE SET
            card_name = EXCLUDED.card_name,
            low_price = EXCLUDED.low_price,
            mid_price = EXCLUDED.mid_price,
            high_price = EXCLUDED.high_price,
            market_price = EXCLUDED.market_price,
            image_url = EXCLUDED.image_url,
            set_name = EXCLUDED.set_name,
            card_number = EXCLUDED.card_number,
            updated_at = CURRENT_TIMESTAMP
        WHERE latest_pokemon_prices.market_price IS DISTINCT FROM EXCLUDED.market_price
           OR latest_pokemon_prices.image_url IS DISTINCT FROM EXCLUDED.image_url
           OR latest_pokemon_prices.set_name IS DISTINCT FROM EXCLUDED.set_name
           OR latest_pokemon_prices.card_number IS DISTINCT FROM EXCLUDED.card_number
        RETURNING card_id, card_name, variant, market_price, low_price, mid_price, high_price;
    """

    # History query: Append only the changed records
    history_query = """
        INSERT INTO pokemon_price_history 
        (card_id, card_name, variant, market_price, low_price, mid_price, high_price)
        VALUES ($1, $2, $3, $4, $5, $6, $7);
    """

    # Downsample query: for history older than 7 days, keep only the latest
    # record per card/variant/day instead of every hourly point, so storage
    # grows ~24x slower while still preserving long-term price trends.
    downsample_query = """
        DELETE FROM pokemon_price_history a
        USING pokemon_price_history b
        WHERE a.fetched_at < NOW() - INTERVAL '7 days'
          AND a.card_id = b.card_id
          AND a.variant = b.variant
          AND date_trunc('day', a.fetched_at) = date_trunc('day', b.fetched_at)
          AND a.fetched_at < b.fetched_at;
    """

    async with conn.transaction():
        # Execute Upserts and capture changed rows
        changed_rows = []
        for r in records:
            row = await conn.fetchrow(upsert_query, *r)
            if row:
                changed_rows.append(tuple(row))

        # Append changed rows to history log
        if changed_rows:
            await conn.executemany(history_query, changed_rows)
            print(f"Appended {len(changed_rows)} price updates to history.")
        else:
            print("No market price changes detected since last run.")

        # Downsample old data
        delete_result = await conn.execute(downsample_query)
        print(f"Downsampled old records: {delete_result}")

    await conn.close()
    print("Database sync complete!")


if __name__ == "__main__":
    asyncio.run(main())
