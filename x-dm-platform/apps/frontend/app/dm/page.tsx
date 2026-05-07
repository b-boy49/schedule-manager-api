'use client';

import { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import Link from 'next/link';

type Conversation = { id: string; messages: { body: string }[] };

export default function DmIndexPage() {
  const [rows, setRows] = useState<Conversation[]>([]);

  useEffect(() => {
    const token = localStorage.getItem('accessToken') || '';
    api<Conversation[]>('/dm/conversations', {}, token).then(setRows).catch(() => setRows([]));
  }, []);

  return (
    <main style={{ maxWidth: 700, margin: '40px auto', padding: 16 }}>
      <h1>Messages</h1>
      {rows.map((c) => (
        <Link key={c.id} href={`/dm/${c.id}`} style={{ display: 'block', padding: 12, borderBottom: '1px solid #2f3336', color: '#e7e9ea' }}>
          <strong>{c.id}</strong>
          <p style={{ margin: 0, color: '#8b98a5' }}>{c.messages[0]?.body || 'No messages yet'}</p>
        </Link>
      ))}
    </main>
  );
}
