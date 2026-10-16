#!/usr/bin/env python3
"""Read-only transport checks against one running MC Tiny Tweak instance."""
import argparse
import base64
import concurrent.futures
import json
import struct
from pathlib import Path
import urllib.error
import urllib.request


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('game_directory', type=Path, help='Instance game directory containing config/')
    args = parser.parse_args()
    config = args.game_directory / 'config'
    settings = dict(line.split('=', 1) for line in (config / 'mc_tiny_tweak_mcp.properties').read_text().splitlines()
                    if '=' in line and not line.startswith('#'))
    token = (config / 'mc_tiny_tweak_mcp_token.txt').read_text().strip()
    endpoint = 'http://127.0.0.1:' + settings.get('port', '25599') + '/mcp'

    def rpc(method, params=None):
        body = json.dumps({'jsonrpc': '2.0', 'id': 1, 'method': method, 'params': params or {}}).encode()
        request = urllib.request.Request(endpoint, body, {'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json'})
        with urllib.request.urlopen(request, timeout=15) as response:
            return json.load(response)

    def check(condition, label):
        if not condition:
            raise RuntimeError('FAIL: ' + label)
        print('PASS:', label)

    initialized = rpc('initialize', {'protocolVersion': '2025-11-25'})
    check(initialized['result']['protocolVersion'] == '2025-11-25', 'protocol negotiation')
    check(rpc('ping')['result'] == {}, 'ping')
    names = {tool['name'] for tool in rpc('tools/list')['result']['tools']}
    check({'get_state', 'get_container', 'click_slot', 'respawn'} <= names, 'gameplay tool discovery (features must be enabled)')
    check(rpc('missing')['error']['code'] == -32601, 'unknown method rejection')
    check(rpc('tools/call', {'name': 'missing'})['error']['code'] == -32602, 'unknown tool rejection')
    for arguments in ({}, {'slot': '2'}, {'slot': 10}, {'slot': None}):
        check(rpc('tools/call', {'name': 'select_slot', 'arguments': arguments})['result']['isError'],
              'invalid slot arguments rejected: ' + repr(arguments))
    check(not rpc('tools/call', {'name': 'get_state'})['result']['isError'], 'state in world or on title screen')
    dimensions = []
    for scale in (2, 4):
        result = rpc('tools/call', {'name': 'screenshot', 'arguments': {'downscale': scale}})['result']
        check(not result['isError'], 'screenshot scale ' + str(scale))
        png = base64.b64decode(next(item['data'] for item in result['content'] if item['type'] == 'image'))
        check(png[:8] == b'\x89PNG\r\n\x1a\n', 'PNG screenshot format')
        dimensions.append(struct.unpack('>II', png[16:24]))
    check(dimensions[1] == tuple(max(1, size // 2) for size in dimensions[0]), 'scale 4 produces half the dimensions of scale 2')
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        check(all(result['result'] == {} for result in pool.map(lambda _: rpc('ping'), range(40))), '40 concurrent pings')
    for label, headers, method, expected in [
        ('missing token', {}, 'POST', 401),
        ('wrong token', {'Authorization': 'Bearer incorrect'}, 'POST', 401),
        ('foreign origin', {'Authorization': 'Bearer ' + token, 'Origin': 'https://example.com'}, 'POST', 403),
        ('foreign host', {'Authorization': 'Bearer ' + token, 'Host': 'example.com'}, 'POST', 403),
        ('unsupported method', {'Authorization': 'Bearer ' + token}, 'GET', 405),
    ]:
        request = urllib.request.Request(endpoint, b'{}' if method == 'POST' else None, headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=15) as response:
                status = response.status
        except urllib.error.HTTPError as error:
            status = error.code
        check(status == expected, label + ' rejection')
    print('All live smoke checks passed. Credentials were kept private.')


if __name__ == '__main__':
    main()
