import express, { Request, Response } from "express";
import crypto from 'node:crypto';
import { Connection } from './types';

const app = express()
const port = 3000

let connections: Connection[] = [];

app.get('/test', eventsHandler);

function eventsHandler(req: Request, res: Response) {
    const headers: Record<string, string> = {
        'Content-Type': 'text/event-stream',
        'Connection': 'keep-alive',
        'Cache-Control': 'no-cache'
    };
    res.writeHead(200, headers);
    const connectionId = crypto.randomUUID();
    const newConnection: Connection = {
        id: connectionId,
        response: res
    };
    connections.push(newConnection);

    sendSessionsToAll();

    req.on('close', () => {
        console.log(`${connectionId} Connection closed`);
        connections = connections.filter(connection => connection.id !== connectionId);
        sendSessionsToAll();
    });
}

function sendSessionsToAll() {
    connections.forEach(c => c.response.write(`event: sessions\ndata: Sessions ${connections.length}\n\n`))
}

app.listen(port, () => {
    console.log(`SSE test listening on port ${port}`)
})