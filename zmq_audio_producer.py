#!/usr/bin/env python3
"""
ZeroMQ Audio Producer Test Tool
Simulates SDRTrunk's ZMQ audio exfiltration service for testing the consumer.
Publishes dummy DMR audio messages with realistic metadata.
"""

import zmq
import json
import base64
import time
import random
import argparse
import numpy as np
from datetime import datetime

def generate_test_audio(duration_ms=1000, sample_rate=8000):
    """Generate test audio data (sine wave + noise)"""
    samples = int(duration_ms * sample_rate / 1000)
    
    # Generate a sine wave with some noise to simulate voice
    t = np.linspace(0, duration_ms/1000, samples)
    frequency = 800 + random.randint(-200, 200)  # Voice-like frequency
    
    # Generate sine wave with amplitude modulation and noise
    audio = 0.3 * np.sin(2 * np.pi * frequency * t)
    audio += 0.1 * np.sin(2 * np.pi * frequency * 1.5 * t)  # Harmonic
    audio += 0.05 * np.random.normal(0, 1, samples)  # Background noise
    
    # Add some amplitude variation to simulate speech
    envelope = 0.5 + 0.5 * np.sin(2 * np.pi * 2 * t)  # 2 Hz modulation
    audio *= envelope
    
    # Convert to 16-bit PCM
    audio = np.clip(audio, -1.0, 1.0)
    pcm_data = (audio * 32767).astype(np.int16)
    
    return pcm_data.tobytes()

def create_test_message(message_id):
    """Create a realistic test message with audio and metadata"""
    
    # Sample radio IDs and talkgroups
    radio_ids = [12345, 23456, 34567, 45678, 56789, 67890]
    talkgroups = [100, 101, 102, 200, 201, 300]
    frequencies = [462675000, 462700000, 462725000, 467675000, 467700000]
    
    # Generate test audio
    audio_data = generate_test_audio(duration_ms=random.randint(500, 2000))
    
    # Create message structure matching SDRTrunk format
    message = {
        "audio": str(list(audio_data)),  # Convert bytes to list representation
        "metadata": {
            "protocol": "DMR",
            "timeslot": random.randint(1, 2),
            "frequency": random.choice(frequencies),
            "from": {
                "id": random.choice(radio_ids),
                "alias": f"Unit {random.randint(1, 20)}"
            },
            "to": {
                "id": random.choice(talkgroups),
                "alias": f"TG {random.choice(['Dispatch', 'Fire', 'EMS', 'Police', 'Ops'])}"
            },
            "lcn": random.randint(1, 10)
        },
        "timestamp": int(time.time() * 1000)
    }
    
    return message

def main():
    parser = argparse.ArgumentParser(description='ZeroMQ Audio Producer Test Tool')
    parser.add_argument('--endpoint', default='tcp://*:15023',
                       help='ZeroMQ endpoint to bind to (default: tcp://*:15023)')
    parser.add_argument('--interval', type=float, default=3.0,
                       help='Interval between messages in seconds (default: 3.0)')
    parser.add_argument('--count', type=int, default=0,
                       help='Number of messages to send (0 = infinite, default: 0)')
    parser.add_argument('--verbose', '-v', action='store_true',
                       help='Enable verbose output')
    
    args = parser.parse_args()
    
    # Create ZMQ context and publisher socket
    context = zmq.Context()
    socket = context.socket(zmq.PUB)
    
    try:
        # Bind to the endpoint
        socket.bind(args.endpoint)
        print(f"ZMQ Audio Producer started on {args.endpoint}")
        print(f"Publishing test messages every {args.interval} seconds")
        print("Press Ctrl+C to stop")
        
        # Allow some time for subscribers to connect
        time.sleep(0.5)
        
        message_count = 0
        
        while True:
            # Check if we've reached the message limit
            if args.count > 0 and message_count >= args.count:
                break
            
            # Create and send test message
            message = create_test_message(message_count + 1)
            json_message = json.dumps(message)
            
            socket.send_string(json_message)
            message_count += 1
            
            if args.verbose:
                timestamp = datetime.fromtimestamp(message['timestamp'] / 1000.0)
                metadata = message['metadata']
                audio_size = len(message['audio'])
                
                print(f"\n[{timestamp.strftime('%H:%M:%S')}] Message #{message_count}")
                print(f"  From: {metadata['from']['id']} ({metadata['from']['alias']})")
                print(f"  To: {metadata['to']['id']} ({metadata['to']['alias']})")
                print(f"  Frequency: {metadata['frequency']} Hz")
                print(f"  Timeslot: {metadata['timeslot']}")
                print(f"  LCN: {metadata['lcn']}")
                print(f"  Audio size: {audio_size} characters")
            else:
                print(f"Sent message #{message_count}")
            
            # Wait for next message
            time.sleep(args.interval)
            
    except KeyboardInterrupt:
        print(f"\nStopping after {message_count} messages...")
    except Exception as e:
        print(f"Error: {e}")
    finally:
        socket.close()
        context.term()
        print("ZMQ Audio Producer stopped")

if __name__ == "__main__":
    # Check if numpy is available, if not provide fallback
    try:
        import numpy as np
    except ImportError:
        print("Warning: numpy not available, using simple audio generation")
        
        def generate_test_audio(duration_ms=1000, sample_rate=8000):
            """Fallback audio generation without numpy"""
            samples = int(duration_ms * sample_rate / 1000)
            audio_data = []
            
            for i in range(samples):
                # Simple sine wave
                t = i / sample_rate
                sample = int(16000 * (0.5 + 0.3 * (i % 100) / 100))  # Simple pattern
                
                # Convert to little-endian 16-bit
                audio_data.append(sample & 0xFF)
                audio_data.append((sample >> 8) & 0xFF)
            
            return bytes(audio_data)
    
    main()
