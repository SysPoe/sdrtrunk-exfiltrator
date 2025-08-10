#!/bin/bash
# ZMQ Audio Test Script
# Demonstrates the ZMQ audio exfiltration system by running a producer and consumer

echo "ZMQ Audio Exfiltration Test"
echo "=========================="
echo
echo "This script will:"
echo "1. Start a ZMQ audio producer (simulating SDRTrunk)"
echo "2. Start a ZMQ audio consumer (for testing)"
echo "3. Show real-time audio messages with metadata"
echo
echo "Requirements:"
echo "- Python 3 with pyzmq (pip install pyzmq)"
echo "- Optional: numpy for better audio generation (pip install numpy)"
echo
echo "Press Enter to continue or Ctrl+C to cancel..."
read

# Check if pyzmq is available
if ! python3 -c "import zmq" 2>/dev/null; then
    echo "Error: pyzmq not installed. Install with: pip install pyzmq"
    exit 1
fi

echo "Starting ZMQ Audio Test..."
echo

# Start producer in background
echo "Starting producer on tcp://*:5555..."
python3 zmq_audio_producer.py --verbose --interval 2 &
PRODUCER_PID=$!

# Give producer time to start
sleep 1

echo "Starting consumer..."
echo "Press Ctrl+C to stop both producer and consumer"
echo

# Start consumer in foreground
python3 zmq_audio_consumer.py --endpoint tcp://localhost:5555 --verbose

# Clean up background producer when consumer exits
echo "Stopping producer..."
kill $PRODUCER_PID 2>/dev/null
wait $PRODUCER_PID 2>/dev/null

echo "Test completed."
