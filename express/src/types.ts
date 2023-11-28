import { UUID } from 'crypto';
import { Response } from "express";

export interface Connection {
    id: UUID,
    response: Response,
}