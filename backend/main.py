from fastapi import FastAPI, Query, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
import httpx
import re
import urllib.parse

app = FastAPI(
    title="SONORA AI Backend API",
    version="1.0.0",
    description="Authorized music search aggregation, NLP voice command processing, and audio discovery proxy for SONORA AI."
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class TrackDto(BaseModel):
    id: str
    title: str
    artist: str
    album: str
    artworkUrl: str
    streamUrl: str
    durationMs: int
    sourceName: str = "Official Audio Stream"
    isPreview: bool = False
    genre: str = ""
    releaseDate: str = ""

class CommandRequest(BaseModel):
    text: str
    language: Optional[str] = "auto"

class CommandResponse(BaseModel):
    action: str # "PLAY", "PAUSE", "RESUME", "NEXT", "PREVIOUS", "FAVORITE", "SEARCH", "UNKNOWN"
    query: Optional[str] = None
    originalText: str
    confidence: float = 1.0

# Curated high quality open licensed fallback tracks
OPEN_TRACKS = [
    TrackDto(
        id="open_lofi_1",
        title="Midnight Coffee Lo-Fi",
        artist="Aesthetic Beats",
        album="Chillhop Sessions Vol. 1",
        artworkUrl="https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80",
        streamUrl="https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=lofi-study-112191.mp3",
        durationMs=147000,
        sourceName="Open Audio (Full Track)",
        isPreview=False,
        genre="Lo-Fi"
    ),
    TrackDto(
        id="open_synth_2",
        title="Cyberpunk Drive",
        artist="Neon Dreamer",
        album="Retro Wave Chronicles",
        artworkUrl="https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
        streamUrl="https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=electronic-future-beats-117997.mp3",
        durationMs=168000,
        sourceName="Open Audio (Full Track)",
        isPreview=False,
        genre="Synthwave"
    ),
    TrackDto(
        id="open_acoustic_3",
        title="Sunset Melodies",
        artist="Acoustic Horizon",
        album="Peaceful Acoustic",
        artworkUrl="https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
        streamUrl="https://cdn.pixabay.com/download/audio/2022/11/06/audio_c976d8b945.mp3?filename=acoustic-guitars-ambient-126216.mp3",
        durationMs=135000,
        sourceName="Open Audio (Full Track)",
        isPreview=False,
        genre="Acoustic"
    )
]

@app.get("/")
def root():
    return {
        "app": "SONORA AI Backend",
        "status": "online",
        "endpoints": ["/health", "/api/search", "/api/trending", "/api/parse-command"]
    }

@app.get("/health")
def health():
    return {"status": "ok", "service": "sonora-ai-backend"}

@app.get("/api/search", response_model=List[TrackDto])
async def search_music(q: str = Query(..., min_length=1), limit: int = Query(25, ge=1, le=50)):
    """
    Search official music catalogs for playable audio streams.
    Returns tracks with direct audio preview/full URLs.
    """
    clean_query = q.strip()
    tracks: List[TrackDto] = []

    # 1. Query iTunes / Apple Music Public Catalog
    try:
        encoded_query = urllib.parse.quote(clean_query)
        url = f"https://itunes.apple.com/search?term={encoded_query}&media=music&entity=song&limit={limit}"
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.get(url, headers={"User-Agent": "SonoraAI-Backend/1.0"})
            if resp.status_code == 200:
                data = resp.json()
                for item in data.get("results", []):
                    preview_url = item.get("previewUrl")
                    if preview_url:
                        raw_art = item.get("artworkUrl100", "")
                        art_600 = raw_art.replace("100x100bb.jpg", "600x600bb.jpg").replace("100x100bb.png", "600x600bb.png")
                        tracks.append(
                            TrackDto(
                                id=f"itunes_{item.get('trackId')}",
                                title=item.get("trackName", "Unknown"),
                                artist=item.get("artistName", "Unknown"),
                                album=item.get("collectionName", item.get("trackName", "Single")),
                                artworkUrl=art_600,
                                streamUrl=preview_url,
                                durationMs=30000,
                                sourceName="Official Audio Preview",
                                isPreview=True,
                                genre=item.get("primaryGenreName", ""),
                                releaseDate=item.get("releaseDate", "")
                            )
                        )
    except Exception as e:
        print(f"iTunes query warning: {e}")

    # 2. Check open catalog if needed
    q_lower = clean_query.lower()
    for ot in OPEN_TRACKS:
        if q_lower in ot.title.lower() or q_lower in ot.artist.lower() or q_lower in ot.genre.lower():
            tracks.append(ot)

    return tracks[:limit]

@app.get("/api/trending", response_model=List[TrackDto])
async def get_trending(category: str = Query("Tamil Hits")):
    cat_query_map = {
        "tamil": "Anirudh Ravichander Tamil hits",
        "tamil hits": "Anirudh Ravichander Tamil hits",
        "thalapathy": "Thalapathy Vijay Tamil",
        "anirudh": "Anirudh Ravichander",
        "romantic": "A.R. Rahman romantic Tamil",
        "trending": "Top Hits 2024",
        "lo-fi": "Lofi chill beats"
    }
    query = cat_query_map.get(category.lower(), category)
    return await search_music(q=query, limit=20)

@app.post("/api/parse-command", response_model=CommandResponse)
def parse_voice_command(req: CommandRequest):
    """
    Parses natural language speech or typed commands (English, Tamil, Tanglish).
    """
    text = req.text.strip().lower()

    # Direct Controls
    if text in ["pause", "stop", "pause song", "stop playback", "niruthu", "pause pannu", "stop pannu", "paatta niruthu"]:
        return CommandResponse(action="PAUSE", originalText=req.text)

    if text in ["resume", "continue", "play again", "unpause", "marupadiyum play pannu", "thodaru"]:
        return CommandResponse(action="RESUME", originalText=req.text)

    if text in ["next", "next song", "skip", "skip song", "adutha paatu", "aduthathu", "skip pannu"]:
        return CommandResponse(action="NEXT", originalText=req.text)

    if text in ["previous", "previous song", "prev", "back", "munthaiya paatu", "pinnadi po"]:
        return CommandResponse(action="PREVIOUS", originalText=req.text)

    if any(k in text for k in ["favorite", "favourite", "add to favorite", "like this song", "save this", "pidichirukku"]):
        return CommandResponse(action="FAVORITE", originalText=req.text)

    # Play prefixes
    play_prefixes = ["play song", "play songs", "play track", "play the song", "play me", "play", "paatu podu", "song podu", "play pannu"]
    for prefix in play_prefixes:
        if text.startswith(prefix):
            q = text[len(prefix):].strip()
            q = re.sub(r"\b(songs|song|tracks|track|paatu|paadal)\b", "", q).strip()
            if q:
                return CommandResponse(action="PLAY", query=q, originalText=req.text)

    # Play suffixes (Tamil / Tanglish)
    play_suffixes = ["play pannu", "paatu podu", "paata podu", "poda sollu", "play"]
    for suffix in play_suffixes:
        if text.endswith(suffix) and len(text) > len(suffix):
            q = text[:-len(suffix)].strip()
            q = re.sub(r"\b(songs|song|tracks|track|paatu|paadal)\b", "", q).strip()
            if q:
                return CommandResponse(action="PLAY", query=q, originalText=req.text)

    # Search prefixes
    search_prefixes = ["search for", "search", "find songs by", "find songs of", "find song", "find", "thedu", "kandupidi"]
    for prefix in search_prefixes:
        if text.startswith(prefix):
            q = text[len(prefix):].strip()
            if q:
                return CommandResponse(action="SEARCH", query=q, originalText=req.text)

    # Fallback to play if string is substantial
    clean_q = re.sub(r"^(the|a|an)\s+", "", text).strip()
    clean_q = re.sub(r"\b(songs|song|tracks|track|paatu|paadal)\b", "", clean_q).strip()
    if len(clean_q) > 1:
        return CommandResponse(action="PLAY", query=clean_q, originalText=req.text)

    return CommandResponse(action="UNKNOWN", query=text, originalText=req.text, confidence=0.5)
