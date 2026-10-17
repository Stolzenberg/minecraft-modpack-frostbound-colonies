#!/usr/bin/env python3
"""Minimaler RCON-Client: rcon.py "<befehl>" ["<befehl>" ...]  (Port 25575, Passwort testtest)"""
import socket, struct, sys


def pkt(sock, rid, typ, body):
    data = struct.pack("<ii", rid, typ) + body.encode() + b"\x00\x00"
    sock.sendall(struct.pack("<i", len(data)) + data)


def recv(sock):
    raw = b""
    while len(raw) < 4:
        raw += sock.recv(4 - len(raw))
    n = struct.unpack("<i", raw)[0]
    buf = b""
    while len(buf) < n:
        buf += sock.recv(n - len(buf))
    return buf[8:-2].decode(errors="replace")


s = socket.create_connection(("127.0.0.1", 25575), timeout=600)
pkt(s, 1, 3, "testtest")
recv(s)
for i, cmd in enumerate(sys.argv[1:], start=10):
    pkt(s, i, 2, cmd)
    print(f"> {cmd}\n{recv(s)}")
