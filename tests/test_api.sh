#!/bin/bash


echo "--- 1. REGISTRIERUNG:"
# POST /api/users/register
curl -X POST http://localhost:8080/api/users/register \
-H "Content-Type: application/json" \
-d '{"username":"testuser", "password":"securepassword"}'
echo ""
echo "-----"


echo "--- 2. LOGIN:"
# POST /api/users/login
RESPONSE=$(curl -s -X POST http://localhost:8080/api/users/login \
-H "Content-Type: application/json" \
-d '{"username":"testuser", "password":"securepassword"}')

# extrahiert den token-wert
AUTH_TOKEN=$(echo $RESPONSE | sed 's/.*"token":"\([^"]*\)".*/\1/')

echo "login antwort: $RESPONSE"
echo "gespeicherter token: $AUTH_TOKEN"
if [ -z "$AUTH_TOKEN" ]; then
    echo "FEHLER: token konnte nicht extrahiert werden. skript wird beendet."
    exit 1
fi
echo "-----"


echo "--- 3. CREATE: "
MEDIA_RESPONSE=$(curl -s -X POST http://localhost:8080/api/media \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $AUTH_TOKEN" \
-d '{"title":"testfilm", "description":"ein film für den test", "mediaType":"MOVIE", "releaseYear":2024, "genres":["Action"], "ageRestriction":16}')

# extrahiert ID des erstellten eintrags
MEDIA_ID=$(echo $MEDIA_RESPONSE | sed 's/.*"id":\([0-9]*\).*/\1/')

echo "create Antwort: $MEDIA_RESPONSE"
echo "erstellter media ID: $MEDIA_ID"
if [ -z "$MEDIA_ID" ]; then
    echo "FEHLER: media ID konnte nicht extrahiert werden. CREATE-Schritt fehlgeschlagen."
    exit 1
fi
echo "-----"


echo "--- 4. READ ALL: "
curl -X GET http://localhost:8080/api/media
echo ""
echo "-----"


echo "--- 5. READ SINGLE: "
# GET /api/media/{id}
curl -X GET http://localhost:8080/api/media/$MEDIA_ID
echo ""
echo "-----"


echo "--- 6. UPDATE: "
curl -X PUT http://localhost:8080/api/media/$MEDIA_ID \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $AUTH_TOKEN" \
-d '{"id":'$MEDIA_ID', "title":"testfilm (aktualisiert)", "description":"verbesserte beschreibung", "mediaType":"MOVIE", "releaseYear":2024, "genres":["Action"], "ageRestriction":16, "creatorId":1}'
echo ""
echo "-----"


echo "--- 7. DELETE: "
curl -X DELETE http://localhost:8080/api/media/$MEDIA_ID \
-H "Authorization: Bearer $AUTH_TOKEN"
echo ""
echo "-----"
