# sdrtrunk-exfiltrator

A fork of sdrtrunk as of 10 August 2025, which exposes DMR metadata and audio in realtime via a ZeroMQ pub-sub server.

**Original Project:** [DSheirer/sdrtrunk](https://github.com/DSheirer/sdrtrunk/)

## ZeroMQ Audio Exfiltration Feature

This fork adds real-time streaming of decoded DMR Tier 3 audio with comprehensive metadata via ZeroMQ. The service provides:

- Radio IDs (FROM) with aliases
- Talkgroup IDs (TO) with aliases  
- Logical Channel Numbers (LCN)
- Frequencies
- Timeslot information
- Timestamps
- PCM 16-bit audio data

### Default Configuration
- **Default Port**: 15023
- **Default Endpoint**: `tcp://*:15023` (binds to all interfaces)
- **Protocol**: TCP over ZeroMQ PUB/SUB pattern

## Quick Start

### 1. Enable ZMQ Audio Exfiltration
1. Open SDRTrunk preferences
2. Navigate to **Audio ZMQ Exfiltration** in the left panel
3. Enable the service with the toggle switch
4. Configure the endpoint (default: `tcp://*:15023`)

### 2. Test the Stream
```bash
# Install Python dependencies
pip install pyzmq

# Start the consumer to receive audio and metadata
python3 zmq_audio_consumer.py --endpoint tcp://localhost:15023 --save-audio --verbose
```

### 3. Message Format
Each ZMQ message contains JSON with audio and metadata:

```json
{
  "audio": "[1, 2, 3, ...]",
  "metadata": {
    "protocol": "DMR",
    "timeslot": 1,
    "frequency": 462675000,
    "from": {
      "id": 12345,
      "alias": "Unit 1"
    },
    "to": {
      "id": 67890,
      "alias": "Dispatch"
    },
    "lcn": 5
  },
  "timestamp": 1703123456789
}
```

## Audio Format
- **Encoding**: PCM 16-bit signed little-endian
- **Sample Rate**: 8000 Hz (typical for DMR)
- **Channels**: Mono (1 channel)
- **Bit Depth**: 16 bits per sample

## Test Tools

Several Python tools are included for testing and development:

- **`zmq_audio_consumer.py`**: Receives and processes audio messages, can save to WAV files
- **`zmq_audio_producer.py`**: Test producer that simulates SDRTrunk audio messages
- **`zmq_scanner.py`**: Scans for active ZMQ endpoints

### Consumer Usage
```bash
# Basic usage (connects to default port 15023)
python3 zmq_audio_consumer.py

# Save audio to WAV files with verbose output
python3 zmq_audio_consumer.py --save-audio --verbose

# Connect to custom endpoint
python3 zmq_audio_consumer.py --endpoint tcp://192.168.1.100:15024
```

### Producer Testing
```bash
# Generate test messages every 3 seconds
python3 zmq_audio_producer.py

# Custom endpoint and faster interval
python3 zmq_audio_producer.py --endpoint tcp://*:15024 --interval 1.0 --verbose

# Send limited number of test messages
python3 zmq_audio_producer.py --count 10 --interval 0.5
```

### Port Scanner
```bash
# Scan default range (15023-15050)
python3 zmq_scanner.py

# Test specific endpoint
python3 zmq_scanner.py --test-endpoint tcp://localhost:15023
```

## Integration Example

Basic Python consumer:

```python
import zmq
import json

context = zmq.Context()
socket = context.socket(zmq.SUB)
socket.connect("tcp://localhost:15023")
socket.setsockopt(zmq.SUBSCRIBE, b"")  # Subscribe to all messages

while True:
    message = socket.recv_string()
    data = json.loads(message)
    
    # Extract metadata
    metadata = data['metadata']
    from_info = metadata.get('from', {})
    to_info = metadata.get('to', {})
    
    print(f"Audio from {from_info.get('alias', from_info.get('id', 'Unknown'))} "
          f"to {to_info.get('alias', to_info.get('id', 'Unknown'))} "
          f"on {metadata.get('frequency', 'Unknown')} Hz")
```

## Security Considerations

- ZeroMQ binding should be configured carefully in production environments
- Consider using `tcp://127.0.0.1:port` for local-only access
- Use `tcp://*:port` only if remote access is required
- No authentication is currently implemented - consider firewall rules for security

## Dependencies

This fork adds:
- **JeroMQ**: Java implementation of ZeroMQ (version 0.6.0)
- **Gson**: JSON serialization library (already included in SDRTrunk)

Python test tools require:
- **pyzmq**: Python ZeroMQ bindings

## Troubleshooting

### Common Issues
1. **Service not starting**: Check endpoint configuration and port availability
2. **No messages received**: Verify ZMQ endpoint matches consumer connection  
3. **Audio quality issues**: Ensure proper PCM 16-bit decoding on consumer side
4. **Port conflicts**: Change the port number in preferences if 15023 is in use

### Logging
Enable debug logging for `io.github.dsheirer.audio.exfiltrate.ZmqAudioExfiltrationService` to see detailed operation logs.

## Original SDRTrunk

This is a fork of the excellent [sdrtrunk](https://github.com/DSheirer/sdrtrunk/) project by Dennis Sheirer. For the original documentation, features, and support:

* [Original Project](https://github.com/DSheirer/sdrtrunk/)
* [Help/Wiki](https://github.com/DSheirer/sdrtrunk/wiki)
* [Getting Started](https://github.com/DSheirer/sdrtrunk/wiki/Getting-Started)
* [User's Manual](https://github.com/DSheirer/sdrtrunk/wiki/User-Manual)
* [Support](https://github.com/DSheirer/sdrtrunk/wiki/Support)

## System Requirements
* **Operating System:** Windows (64-bit), Linux (64-bit) or Mac (64-bit, 12.x or higher)
* **CPU:** 4-core
* **RAM:** 8GB or more (preferred). Depending on usage, 4GB may be sufficient.
