import { Suspense, use, useEffect, useMemo, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";

const API_BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";
const WS_URL = import.meta.env.VITE_WS_URL ?? "ws://localhost:8080/ws";

const ROWS = 10;
const COLS = 10;
const LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

const seatsPromise = fetch(`${API_BASE}/api/v1/seats`)
  .then((res) => (res.ok ? res.json() : []))
  .catch(() => []);

const buildGrid = () => {
  const seats = [];
  for (let r = 0; r < ROWS; r += 1) {
    for (let c = 0; c < COLS; c += 1) {
      seats.push({
        seatId: r * COLS + c + 1,
        seatNumber: `${LETTERS[r]}-${c + 1}`,
        booked: false,
        bookedBy: null,
        bookedAt: null,
      });
    }
  }
  return seats;
};

const mergeSeats = (initialSeats) => {
  const base = buildGrid();
  const byNumber = new Map(
    (initialSeats ?? []).map((seat) => [seat.seatNumber, seat])
  );
  return base.map((seat) => ({
    ...seat,
    ...(byNumber.get(seat.seatNumber) ?? {}),
  }));
};

const readErrorMessage = async (response) => {
  if (!response) {
    return "Request failed.";
  }
  try {
    const payload = await response.json();
    return payload?.message ?? `Request failed (${response.status}).`;
  } catch {
    return `Request failed (${response.status}).`;
  }
};

const bookSeat = async (seatId, userId) => {
  const response = await fetch(`${API_BASE}/api/v1/bookings`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ seatId, userId, strategy: "REDIS" }),
  });

  if (!response.ok) {
    const message = await readErrorMessage(response);
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  return response.json();
};

function SeatMapInner() {
  const initialSeats = use(seatsPromise);
  const [seats, setSeats] = useState(() => mergeSeats(initialSeats));
  const [selectedSeatId, setSelectedSeatId] = useState(
    () => mergeSeats(initialSeats)[0]?.seatId ?? 1
  );
  const [toast, setToast] = useState(null);
  const toastTimer = useRef(null);

  const seatMap = useMemo(() => {
    const map = new Map();
    seats.forEach((seat) => {
      map.set(seat.seatId, seat);
    });
    return map;
  }, [seats]);

  const showToast = (message, variant = "info") => {
    setToast({ message, variant });
    if (toastTimer.current) {
      clearTimeout(toastTimer.current);
    }
    toastTimer.current = setTimeout(() => setToast(null), 3200);
  };

  useEffect(() => {
    const client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 2000,
    });

    client.onConnect = () => {
      client.subscribe("/topic/seats", (message) => {
        const update = JSON.parse(message.body);
        setSeats((prev) =>
          prev.map((seat) =>
            seat.seatId === update.seatId || seat.seatNumber === update.seatNumber
              ? { ...seat, ...update }
              : seat
          )
        );
      });
    };

    client.onStompError = (frame) => {
      showToast(frame.headers?.message ?? "WebSocket error", "error");
    };

    client.activate();
    return () => client.deactivate();
  }, []);

  const handleSeatClick = async (seat) => {
    setSelectedSeatId(seat.seatId);
    try {
      await bookSeat(seat.seatId, `user-${crypto.randomUUID()}`);
    } catch (error) {
      if (error.status === 409) {
        showToast(error.message, "conflict");
      } else {
        showToast(error.message, "error");
      }
    }
  };

  const runChaosMode = async () => {
    const seatId = selectedSeatId ?? 1;
    const requests = Array.from({ length: 20 }, (_, i) =>
      bookSeat(seatId, `chaos-${i}-${Date.now()}`).then(
        () => ({ ok: true }),
        (error) => ({ ok: false, error })
      )
    );

    const results = await Promise.all(requests);
    const successCount = results.filter((r) => r.ok).length;
    showToast(
      `Chaos Mode: ${successCount} succeeded, ${20 - successCount} failed`,
      "info"
    );
  };

  const selectedSeat = seatMap.get(selectedSeatId);

  return (
    <div className="mx-auto flex min-h-screen max-w-6xl flex-col gap-6 px-6 py-10">
      <header className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.4em] text-slate-400">
            Ticket Booking Engine
          </p>
          <h1 className="mt-2 text-3xl font-semibold text-white md:text-4xl">
            Live Seat Control Panel
          </h1>
          <p className="mt-2 max-w-xl text-sm text-slate-400">
            Real-time updates powered by STOMP, Redis locks, and Spring Boot.
          </p>
        </div>
        <div className="flex flex-col items-start gap-3 md:items-end">
          <button
            onClick={runChaosMode}
            className="rounded-full bg-indigo-500/90 px-5 py-2 text-sm font-semibold text-white shadow-lg shadow-indigo-500/30 transition hover:bg-indigo-400"
          >
            Chaos Mode (20x)
          </button>
          <div className="text-xs text-slate-400">
            Selected: {selectedSeat?.seatNumber ?? "-"}
          </div>
        </div>
      </header>

      <section className="rounded-3xl border border-slate-800/80 bg-slate-950/70 p-6 shadow-xl shadow-slate-900/40">
        <div className="grid grid-cols-5 gap-2 sm:grid-cols-10">
          {seats.map((seat) => {
            const isSelected = seat.seatId === selectedSeatId;
            const base = seat.booked
              ? "bg-rose-500/90 hover:bg-rose-400"
              : "bg-emerald-500/90 hover:bg-emerald-400";
            const ring = isSelected
              ? "ring-2 ring-indigo-400 ring-offset-2 ring-offset-slate-950"
              : "";

            return (
              <button
                key={seat.seatNumber}
                onClick={() => handleSeatClick(seat)}
                className={`h-10 rounded-lg text-[10px] font-semibold text-white transition ${base} ${ring}`}
                title={seat.booked ? `Booked by ${seat.bookedBy}` : "Available"}
              >
                {seat.seatNumber}
              </button>
            );
          })}
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-3">
        <div className="rounded-2xl border border-slate-800/80 bg-slate-950/70 p-5">
          <p className="text-xs uppercase tracking-[0.3em] text-slate-500">
            Seat Status
          </p>
          <p className="mt-3 text-lg font-semibold text-white">
            {selectedSeat?.seatNumber ?? "-"}
          </p>
          <p className="mt-1 text-sm text-slate-400">
            {selectedSeat?.booked
              ? `Booked by ${selectedSeat.bookedBy}`
              : "Available for booking"}
          </p>
        </div>
        <div className="rounded-2xl border border-slate-800/80 bg-slate-950/70 p-5">
          <p className="text-xs uppercase tracking-[0.3em] text-slate-500">
            Strategy
          </p>
          <p className="mt-3 text-lg font-semibold text-white">Redis Lock</p>
          <p className="mt-1 text-sm text-slate-400">
            Fast-fail distributed lock in 2s
          </p>
        </div>
        <div className="rounded-2xl border border-slate-800/80 bg-slate-950/70 p-5">
          <p className="text-xs uppercase tracking-[0.3em] text-slate-500">
            Conflict Toast
          </p>
          <p className="mt-3 text-lg font-semibold text-white">409 Conflict</p>
          <p className="mt-1 text-sm text-slate-400">
            Appears when a seat is already booked
          </p>
        </div>
      </section>

      {toast && (
        <div
          className={`fixed right-6 top-6 rounded-2xl border px-4 py-3 text-sm shadow-lg transition
            ${toast.variant === "conflict"
              ? "border-rose-500/60 bg-rose-500/20 text-rose-100"
              : toast.variant === "error"
              ? "border-amber-400/60 bg-amber-400/20 text-amber-100"
              : "border-slate-600/60 bg-slate-800/70 text-slate-100"}`}
        >
          <div className="text-xs uppercase tracking-[0.3em] text-slate-300">
            Booking Conflict
          </div>
          <div className="mt-1 font-medium">{toast.message}</div>
        </div>
      )}
    </div>
  );
}

export default function SeatMap() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center text-sm text-slate-400">
          Loading seats...
        </div>
      }
    >
      <SeatMapInner />
    </Suspense>
  );
}






