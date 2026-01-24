#!/bin/bash

PASSWORD=${1:-$(read -s -p "Enter password: " && echo "$REPLY")}
if [ -z "$PASSWORD" ]; then
    echo "❌ Password cannot be empty"
    exit 1
fi

python3 -c "
import bcrypt
salt = bcrypt.gensalt()
hashed = bcrypt.hashpw(PASSWORD.encode('utf-8'), salt)
print(hashed.decode('utf-8'))
" 2>/dev/null || {
    echo "❌ Failed to generate hash with Python bcrypt, using fallback method"
    echo "$PASSWORD" | openssl passwd -6 -stdin
}