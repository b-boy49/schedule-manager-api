'use client';

import { useEffect, useMemo, useState } from 'react';
import { api } from '@/lib/api';
import { createSocket } from '@/lib/socket';

export default function ConversationPage({ params }: { params: { conversationId: string } }) {
  const [messages, setMessages] = useState<{ id: string; body: string }[]>([]);
  const [text, setText] = useState('');
  const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') || '' : '';
  const socket = useMemo(() => (token ? createSocket(token) : null), [token]);

  useEffect(() => {
    if (!token) return;
    api<{ id: string; body: string }[]>(`/dm/conversations/${params.conversationId}/messages`, {}, token).then(setMessages);
  }, [params.conversationId, token]);

  useEffect(() => {
    if (!socket) return;
    socket.emit('conversation:join', { conversationId: params.conversationId });
    socket.on('message:new', (payload: { conversationId: string; message: { id: string; body: string } }) => {
      if (payload.conversationId === params.conversationId) {
        setMessages((prev) => [...prev, payload.message]);
      }
    });
    return () => socket.disconnect();
  }, [params.conversationId, socket]);

  const send = () => {
    if (!socket || !text.trim()) return;
    socket.emit('message:send', { conversationId: params.conversationId, body: text });
    setText('');
  };

  return (
    <main style={{ maxWidth: 700, margin: '30px auto', padding: 16 }}>
      <h1>Conversation</h1>
      <section style={{ minHeight: 400, border: '1px solid #2f3336', borderRadius: 8, padding: 12 }}>
        {messages.map((m) => (
          <p key={m.id} style={{ background: '#17202a', padding: 8, borderRadius: 8 }}>{m.body}</p>
        ))}
      </section>
      <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
        <input value={text} onChange={(e) => setText(e.target.value)} style={{ flex: 1, padding: 10 }} />
        <button onClick={send} style={{ padding: '10px 16px', background: '#1d9bf0', color: '#fff', border: 0, borderRadius: 8 }}>Send</button>
      </div>
    </main>
  );
}
