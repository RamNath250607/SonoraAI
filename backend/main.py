from fastapi import FastAPI, Query, HTTPException, UploadFile, File, Form, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import RedirectResponse
from pydantic import BaseModel
from typing import List, Optional
import httpx
import re
import os
import json
import uuid
import urllib.parse
import subprocess
import asyncio

app = FastAPI(
    title="SONORA AI Backend API",
    version="1.3.0",
    description="Authorized music search aggregation, full-song stream resolution, host server storage, live radio, and NLP command processing."
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MEDIA_DIR = os.path.join(BASE_DIR, "media")
META_FILE = os.path.join(MEDIA_DIR, "songs_meta.json")
os.makedirs(MEDIA_DIR, exist_ok=True)

# Mount media directory for static audio streaming with HTTP Range support
app.mount("/media", StaticFiles(directory=MEDIA_DIR), name="media")

class TrackDto(BaseModel):
    id: str
    title: str
    artist: str
    album: str
    artworkUrl: str
    streamUrl: str
    durationMs: int
    sourceName: str = "Full Song Stream"
    isPreview: bool = False
    isLive: bool = False
    genre: str = ""
    releaseDate: str = ""

class RadioStationDto(BaseModel):
    id: str
    name: str
    tags: str
    streamUrl: str
    favicon: str
    country: str = "India"
    language: str = "Tamil"
    bitrate: int = 128
    isLive: bool = True

class CommandRequest(BaseModel):
    text: str
    language: Optional[str] = "auto"

class CommandResponse(BaseModel):
    action: str
    query: Optional[str] = None
    originalText: str
    confidence: float = 1.0

# Memory cache for resolved full songs
FULL_SONG_CACHE = {}

# Curated High-Quality Live Radio Stations
CURATED_RADIO_STATIONS: List[RadioStationDto] = [
    RadioStationDto(
        id="radio_tamil_sun",
        name="Tamil Sun FM",
        tags="Tamil, Super Hits, 24/7",
        streamUrl="https://stream.zeno.fm/f3wvbbqmdg8uv",
        favicon="https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=200",
        country="India",
        language="Tamil"
    ),
    RadioStationDto(
        id="radio_ilayaraja",
        name="Ilayaraja 24/7 Radio",
        tags="Classic, Ilayaraja, Tamil Melodies",
        streamUrl="https://stream.zeno.fm/c30b207uvp8uv",
        favicon="https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=200",
        country="India",
        language="Tamil"
    ),
    RadioStationDto(
        id="radio_ar_rahman",
        name="A.R. Rahman Hits FM",
        tags="ARR, Oscar Winner, Tamil, World",
        streamUrl="https://stream.zeno.fm/4vrtz37uvp8uv",
        favicon="https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=200",
        country="India",
        language="Tamil"
    ),
    RadioStationDto(
        id="radio_panpalai",
        name="Tamil Panpalai FM",
        tags="Talk, News, Retro Tamil",
        streamUrl="https://stream.zeno.fm/s4kqq73c1nhvv",
        favicon="https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=200",
        country="India",
        language="Tamil"
    ),
    RadioStationDto(
        id="radio_lofi_girl",
        name="Lofi Girl 24/7 Beats",
        tags="Lo-Fi, Study, Relax, Chillhop",
        streamUrl="https://play.streamafrica.net/lofigirl",
        favicon="https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=200",
        country="Global",
        language="Instrumental"
    ),
    RadioStationDto(
        id="radio_nightwave",
        name="Nightwave Plaza Synthwave",
        tags="Synthwave, Vaporwave, Cyberpunk",
        streamUrl="https://radio.plaza.one/mp3",
        favicon="https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=200",
        country="Global",
        language="Electronic"
    )
]

def load_hosted_songs() -> List[dict]:
    if os.path.exists(META_FILE):
        try:
            with open(META_FILE, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            return []
    return []

def save_hosted_songs(songs: List[dict]):
    with open(META_FILE, "w", encoding="utf-8") as f:
        json.dump(songs, f, indent=2, ensure_ascii=False)

async def resolve_full_song_stream(query: str) -> Optional[TrackDto]:
    clean = query.strip()
    key = clean.lower()
    if key in FULL_SONG_CACHE:
        return FULL_SONG_CACHE[key]

    loop = asyncio.get_event_loop()
    def _fetch():
        try:
            cmd = [
                "yt-dlp",
                "-f", "ba[ext=m4a]/ba",
                "--dump-json",
                f"ytsearch1:{clean} song"
            ]
            res = subprocess.run(cmd, capture_output=True, text=True, timeout=12)
            if res.returncode == 0 and res.stdout:
                data = json.loads(res.stdout)
                stream_url = data.get("url")
                if stream_url:
                    track = TrackDto(
                        id=f"full_{data.get('id', uuid.uuid4().hex[:8])}",
                        title=data.get("title", clean),
                        artist=data.get("uploader", "Music Artist").replace(" - Topic", ""),
                        album="Full Song Release",
                        artworkUrl=data.get("thumbnail") or "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600",
                        streamUrl=stream_url,
                        durationMs=int(data.get("duration", 240) * 1000),
                        sourceName="Full Song Stream",
                        isPreview=False,
                        isLive=False,
                        genre="Soundtrack"
                    )
                    FULL_SONG_CACHE[key] = track
                    return track
        except Exception as e:
            print(f"Full song resolve error: {e}")
        return None

    return await loop.run_in_executor(None, _fetch)

@app.get("/")
def root():
    return {
        "app": "SONORA AI Backend",
        "version": "1.3.0",
        "status": "online",
        "features": ["full-song-stream", "full-song-host", "live-radio", "nlp-voice", "cross-platform-pwa"]
    }

@app.get("/health")
def health():
    return {"status": "ok", "service": "sonora-ai-backend"}

@app.get("/api/full-song", response_model=TrackDto)
async def get_full_song(q: str = Query(..., min_length=1)):
    """
    Directly resolves and returns a FULL LENGTH (3-5 min) playable audio stream for any query.
    """
    track = await resolve_full_song_stream(q)
    if not track:
        raise HTTPException(status_code=404, detail="Full song stream could not be resolved.")
    return track

@app.get("/api/hosted-songs", response_model=List[TrackDto])
def get_hosted_songs(request: Request):
    base_url = str(request.base_url).rstrip("/")
    raw_songs = load_hosted_songs()
    results: List[TrackDto] = []

    for item in raw_songs:
        stream_url = item.get("streamUrl")
        if item.get("fileName") and os.path.exists(os.path.join(MEDIA_DIR, item.get("fileName"))):
            stream_url = f"{base_url}/media/{item.get('fileName')}"

        results.append(
            TrackDto(
                id=item.get("id", str(uuid.uuid4())),
                title=item.get("title", "Untitled Track"),
                artist=item.get("artist", "Host Artist"),
                album=item.get("album", "Host Server Vault"),
                artworkUrl=item.get("artworkUrl", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600"),
                streamUrl=stream_url or "",
                durationMs=item.get("durationMs", 180000),
                sourceName="Host Server (Full Song)",
                isPreview=False,
                genre=item.get("genre", "Music")
            )
        )
    return results

@app.post("/api/upload-song", response_model=TrackDto)
async def upload_song(
    request: Request,
    file: UploadFile = File(...),
    title: str = Form(...),
    artist: str = Form(...),
    album: str = Form("Host Uploads"),
    genre: str = Form("User Music")
):
    ext = os.path.splitext(file.filename)[1].lower() or ".mp3"
    clean_name = f"{uuid.uuid4()}{ext}"
    dest_path = os.path.join(MEDIA_DIR, clean_name)

    contents = await file.read()
    with open(dest_path, "wb") as f:
        f.write(contents)

    base_url = str(request.base_url).rstrip("/")
    stream_url = f"{base_url}/media/{clean_name}"
    song_id = f"hosted_{uuid.uuid4().hex[:8]}"

    artwork = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600"
    new_entry = {
        "id": song_id,
        "title": title.strip(),
        "artist": artist.strip(),
        "album": album.strip(),
        "artworkUrl": artwork,
        "fileName": clean_name,
        "streamUrl": stream_url,
        "durationMs": 210000,
        "sourceName": "Host Server (Full Song)",
        "isPreview": False,
        "genre": genre
    }

    current = load_hosted_songs()
    current.insert(0, new_entry)
    save_hosted_songs(current)

    return TrackDto(
        id=new_entry["id"],
        title=new_entry["title"],
        artist=new_entry["artist"],
        album=new_entry["album"],
        artworkUrl=new_entry["artworkUrl"],
        streamUrl=new_entry["streamUrl"],
        durationMs=new_entry["durationMs"],
        sourceName="Host Server (Full Song)",
        isPreview=False,
        genre=new_entry["genre"]
    )

@app.get("/api/radio/stations", response_model=List[RadioStationDto])
async def get_radio_stations(tag: Optional[str] = None):
    stations = list(CURATED_RADIO_STATIONS)

    if tag and tag.lower() not in ["all", "curated"]:
        try:
            url = f"https://de1.api.radio-browser.info/json/stations/bytag/{urllib.parse.quote(tag.lower())}?limit=15"
            async with httpx.AsyncClient(timeout=6.0) as client:
                res = await client.get(url, headers={"User-Agent": "SonoraAI/1.0"})
                if res.status_code == 200:
                    for item in res.json():
                        stream_url = item.get("url_resolved") or item.get("url")
                        if stream_url:
                            stations.append(
                                RadioStationDto(
                                    id=f"rb_{item.get('stationuuid')}",
                                    name=item.get("name", "Online Radio"),
                                    tags=item.get("tags", tag),
                                    streamUrl=stream_url,
                                    favicon=item.get("favicon") or "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=200",
                                    country=item.get("country", "India"),
                                    language=item.get("language", tag)
                                )
                            )
        except Exception as e:
            print(f"Radio browser warning: {e}")

    return stations

@app.get("/api/search", response_model=List[TrackDto])
async def search_music(request: Request, q: str = Query(..., min_length=1), limit: int = Query(25, ge=1, le=50)):
    clean_query = q.strip()
    tracks: List[TrackDto] = []

    # 1. Check Host Server Full Songs
    hosted = get_hosted_songs(request)
    q_lower = clean_query.lower()
    for hs in hosted:
        if q_lower in hs.title.lower() or q_lower in hs.artist.lower() or q_lower in hs.genre.lower() or q_lower in hs.album.lower():
            tracks.append(hs)

    # 2. Resolve FULL SONG stream via backend resolver (Highest Priority for Playback!)
    full_track = await resolve_full_song_stream(clean_query)
    if full_track:
        tracks.insert(0, full_track)

    # 3. Add Apple Music / iTunes Public Catalog items
    try:
        encoded_query = urllib.parse.quote(clean_query)
        url = f"https://itunes.apple.com/search?term={encoded_query}&media=music&entity=song&limit={limit}"
        async with httpx.AsyncClient(timeout=8.0) as client:
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

    # Deduplicate by ID
    seen_ids = set()
    distinct_tracks = []
    for t in tracks:
        if t.id not in seen_ids:
            seen_ids.add(t.id)
            distinct_tracks.append(t)

    return distinct_tracks[:limit]

@app.get("/api/trending", response_model=List[TrackDto])
async def get_trending(request: Request, category: str = Query("Tamil Hits")):
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
    return await search_music(request, q=query, limit=20)

@app.post("/api/parse-command", response_model=CommandResponse)
def parse_voice_command(req: CommandRequest):
    text = req.text.strip().lower()

    if text in ["pause", "stop", "pause song", "stop playback", "niruthu", "pause pannu", "stop pannu", "paatta niruthu"]:
        return CommandResponse(action="PAUSE", originalText=req.text)

    if text in ["resume", "continue", "play again", "unpause", "marupadiyum play pannu", "thodaru"]:
        return CommandResponse(action="RESUME", originalText=req.text)

    if text in ["next", "next song", "skip", "skip song", "adutha paatu", "aduthathu", "skip pannu"]:
        return CommandResponse(action="NEXT", originalText=req.text)

    if text in ["previous", "previous song", "prev", "back", "munthaiya paatu", "pinnadi po"]:
        return CommandResponse(action="PREVIOUS", originalText=req.text)

    if any(k in text for k in ["radio", "fm", "live radio", "radio podu"]):
        return CommandResponse(action="RADIO", originalText=req.text)

    if any(k in text for k in ["favorite", "favourite", "add to favorite", "like this song", "save this", "pidichirukku"]):
        return CommandResponse(action="FAVORITE", originalText=req.text)

    play_prefixes = ["play song", "play songs", "play track", "play the song", "play me", "play", "paatu podu", "song podu", "play pannu"]
    for prefix in play_prefixes:
        if text.startswith(prefix):
            q = text[len(prefix):].strip()
            q = re.sub(r"\b(songs|song|tracks|track|paatu|paadal)\b", "", q).strip()
            if q:
                return CommandResponse(action="PLAY", query=q, originalText=req.text)

    play_suffixes = ["play pannu", "paatu podu", "paata podu", "poda sollu", "play"]
    for suffix in play_suffixes:
        if text.endswith(suffix) and len(text) > len(suffix):
            q = text[:-len(suffix)].strip()
            q = re.sub(r"\b(songs|song|tracks|track|paatu|paadal)\b", "", q).strip()
            if q:
                return CommandResponse(action="PLAY", query=q, originalText=req.text)

    search_prefixes = ["search for", "search", "find songs by", "find songs of", "find song", "find", "thedu", "kandupidi"]
    for prefix in search_prefixes:
        if text.startswith(prefix):
            q = text[len(prefix):].strip()
            if q:
                return CommandResponse(action="SEARCH", query=q, originalText=req.text)

    clean_q = re.sub(r"^(the|a|an)\s+", "", text).strip()
    clean_q = re.sub(r"\b(songs|song|tracks|track|paatu|paadal)\b", "", clean_q).strip()
    if len(clean_q) > 1:
        return CommandResponse(action="PLAY", query=clean_q, originalText=req.text)

    return CommandResponse(action="UNKNOWN", query=text, originalText=req.text, confidence=0.5)
