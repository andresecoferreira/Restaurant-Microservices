#!/bin/bash

# ============================================================
# DATA SEEDER SCRIPT FOR ANALYTICS DEMO
# Populates Reservation System with realistic dummy data
# OPTIMIZED: Guarantees 100% Future Dates & Even Distribution
# ============================================================

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
RESTAURANT_SERVICE="http://localhost:8081"
RESERVATION_SERVICE="http://localhost:8082"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  SEEDING DATA FOR ANALYTICS DEMO${NC}"
echo -e "${BLUE}========================================${NC}\n"

# ============================================================
# PHASE 1: CREATE RESTAURANTS
# ============================================================

echo -e "${BLUE}[PHASE 1] Creating 5 Restaurants...${NC}\n"

RESTAURANT_IDS=()
restaurants=("Mario's Italian" "Sushi Zen" "Burger King" "Lisbon Tacos" "Curry House")

for restaurant_name in "${restaurants[@]}"; do
    printf "  Creating restaurant: %-20s ... " "$restaurant_name"

    response=$(curl -s -X POST "$RESTAURANT_SERVICE/api/restaurants" \
        -H "Content-Type: application/json" \
        -d "{\"name\": \"$restaurant_name\", \"city\": \"Lisbon\", \"country\": \"Portugal\", \"phone\": \"+351912345678\", \"email\": \"contact@${restaurant_name// /_}.com\", \"active\": true}")

    restaurant_id=$(echo "$response" | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')

    if [ -z "$restaurant_id" ]; then
        echo -e "${RED}FAILED${NC}"
        continue
    fi

    RESTAURANT_IDS+=($restaurant_id)
    echo -e "${GREEN}✓ ID: $restaurant_id${NC}"
    sleep 0.1
done

echo ""

if [ ${#RESTAURANT_IDS[@]} -eq 0 ]; then
    echo -e "${RED}ERROR: No restaurants created. Exiting.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Created ${#RESTAURANT_IDS[@]} restaurants${NC}\n"

# ============================================================
# PHASE 2: CREATE AVAILABILITY SLOTS
# ============================================================

echo -e "${BLUE}[PHASE 2] Creating Availability Slots (FUTURE ONLY)...${NC}\n"

# FIX: Start from TOMORROW to avoid "Past Date" validation errors
DATE_1=$(date -d "+1 day" +%Y-%m-%d 2>/dev/null || date -v+1d +%Y-%m-%d)
DATE_2=$(date -d "+2 days" +%Y-%m-%d 2>/dev/null || date -v+2d +%Y-%m-%d)
DATE_3=$(date -d "+3 days" +%Y-%m-%d 2>/dev/null || date -v+3d +%Y-%m-%d)

TIMES=("12:00" "13:00" "19:00" "20:00")
SLOT_IDS=()
slot_count=0

for restaurant_id in "${RESTAURANT_IDS[@]}"; do
    for date in "$DATE_1" "$DATE_2" "$DATE_3"; do
        for time in "${TIMES[@]}"; do
            # Calculate end time (1 hour after start)
            start_hour=${time%:*}
            end_hour=$((start_hour + 1))
            end_time=$(printf "%02d:00" $end_hour)

            printf "  Restaurant $restaurant_id | $date @ $time ... "

            response=$(curl -s -X POST "$RESTAURANT_SERVICE/api/restaurants/$restaurant_id/slots" \
                -H "Content-Type: application/json" \
                -d "{\"date\": \"$date\", \"startTime\": \"$time\", \"endTime\": \"$end_time\", \"capacity\": 50, \"seatsAvailable\": 50}")
                # Increased Capacity to 50 to prevent overbooking during demo

            slot_id=$(echo "$response" | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')

            if [ -z "$slot_id" ]; then
                echo -e "${YELLOW}⚠ SKIPPED${NC}"
                continue
            fi

            SLOT_IDS+=($slot_id)
            ((slot_count++))
            echo -e "${GREEN}✓ Slot: $slot_id${NC}"
            sleep 0.05
        done
    done
done

echo ""

if [ ${#SLOT_IDS[@]} -eq 0 ]; then
    echo -e "${RED}ERROR: No slots created. Exiting.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Created $slot_count availability slots${NC}\n"

# ============================================================
# PHASE 3: CREATE RECURRING USER PROFILES
# ============================================================

echo -e "${BLUE}[PHASE 3] Creating User Profiles...${NC}\n"

declare -a USER_EMAILS=(
    "alice.smith@gmail.com"
    "bob.johnson@gmail.com"
    "carol.davies@gmail.com"
    "david.brown@gmail.com"
    "emma.wilson@gmail.com"
    "frank.miller@gmail.com"
    "grace.taylor@gmail.com"
    "henry.clark@gmail.com"
)

declare -a RESERVATION_IDS=()

echo -e "${CYAN}Registered Users:${NC}"
for email in "${USER_EMAILS[@]}"; do
    echo -e "  ${GREEN}✓${NC} $email"
done
echo ""

# ============================================================
# PHASE 4: CREATE RESERVATIONS (Round Robin Distribution)
# ============================================================

echo -e "${BLUE}[PHASE 4] Creating Reservations (Optimized)...${NC}\n"

pending_count=0
confirmed_count=0
cancelled_count=0
failed_count=0
reservation_index=0

# Calculate how many slots we have
total_slots=${#SLOT_IDS[@]}

echo -e "${CYAN}Creating 40 reservations across ${#USER_EMAILS[@]} users...${NC}\n"

for booking_num in {1..40}; do
    # Pick user
    user_index=$(((booking_num - 1) % ${#USER_EMAILS[@]}))
    user_email="${USER_EMAILS[$user_index]}"

    # FIX: Round Robin slot selection instead of random
    # This guarantees we use different slots and avoid "Full Capacity" errors
    slot_index=$(((booking_num - 1) % total_slots))
    target_slot=${SLOT_IDS[$slot_index]}

    # Determine which restaurant this slot belongs to (approximate logic for logging)
    # We don't strictly need the restaurant ID for the POST request, but good for debug
    # Just picking a random restaurant ID for the log message won't hurt the API call if logic ignores it
    # BUT: The API requires restaurantId. Let's map it correctly or just use a safe fallback.
    # Since we don't have a map, we can rely on the fact we created slots sequentially.
    # Actually, the API requires restaurantId. Let's just pick one valid one.
    # The Reservation Service validates if slot belongs to restaurant? Usually yes.
    # Let's act smart: We generated slots iterating restaurants.
    # So: slot_index / (slots_per_restaurant) = restaurant_index.
    # 3 dates * 4 times = 12 slots per restaurant.
    rest_index=$(( slot_index / 12 ))
    target_restaurant=${RESTAURANT_IDS[$rest_index]}

    # Generate party size (2-6)
    party_size=$((2 + RANDOM % 5))

    # Create reservation
    printf "  %2d/40 | User: %-25s | Party: %d | Slot: %-3d ... " \
        "$booking_num" "$user_email" "$party_size" "$target_slot"

    response=$(curl -s -X POST "$RESERVATION_SERVICE/api/reservations" \
        -H "Content-Type: application/json" \
        -d "{\"restaurantId\": $target_restaurant, \"slotId\": $target_slot, \"userEmail\": \"$user_email\", \"partySize\": $party_size}")

    # Extract reservation ID from response
    reservation_id=$(echo "$response" | grep -o '"id":"[^"]*"' | head -1 | sed 's/"id":"//' | sed 's/"//')

    if [ -z "$reservation_id" ]; then
        echo -e "${YELLOW}⚠ FAILED${NC}"
        ((failed_count++))
        sleep 0.1
        continue
    fi

    RESERVATION_IDS+=($reservation_id)
    echo -e "${GREEN}✓ ID: $reservation_id${NC}"

    # Determine status distribution:
    # 50% PENDING, 35% CONFIRMED, 15% CANCELLED
    status_rand=$((RANDOM % 100))

    if [ $status_rand -lt 50 ]; then
        # Leave as PENDING (default status)
        ((pending_count++))
        status="PENDING"
    elif [ $status_rand -lt 85 ]; then
        # Confirm reservation
        sleep 0.1
        printf "      ↳ Confirming reservation ... "
        confirm_response=$(curl -s -X POST "$RESERVATION_SERVICE/api/reservations/$reservation_id/confirm" \
            -H "Content-Type: application/json")

        confirm_status=$(echo "$confirm_response" | grep -o '"status":"[^"]*"' | head -1 | sed 's/"status":"//' | sed 's/"//')
        if [ "$confirm_status" = "CONFIRMED" ]; then
            echo -e "${GREEN}✓ CONFIRMED${NC}"
            ((confirmed_count++))
            status="CONFIRMED"
        else
            echo -e "${YELLOW}⚠ FAILED${NC}"
            ((pending_count++))
        fi
    else
        # Cancel reservation
        sleep 0.1
        printf "      ↳ Cancelling reservation ... "
        cancel_response=$(curl -s -X POST "$RESERVATION_SERVICE/api/reservations/$reservation_id/cancel" \
            -H "Content-Type: application/json")

        cancel_status=$(echo "$cancel_response" | grep -o '"status":"[^"]*"' | head -1 | sed 's/"status":"//' | sed 's/"//')
        if [ "$cancel_status" = "CANCELLED" ]; then
            echo -e "${RED}✓ CANCELLED${NC}"
            ((cancelled_count++))
            status="CANCELLED"
        else
            echo -e "${YELLOW}⚠ FAILED${NC}"
            ((pending_count++))
        fi
    fi

    sleep 0.1
done

echo ""

# ============================================================
# PHASE 5: VERIFY DATA
# ============================================================

echo -e "${BLUE}[PHASE 5] Retrieving All Reservations...${NC}\n"

all_reservations=$(curl -s -X GET "$RESERVATION_SERVICE/api/reservations" \
    -H "Content-Type: application/json")

total_reservations=$(echo "$all_reservations" | grep -o '"id":"[^"]*"' | wc -l)

echo -e "${CYAN}Total Reservations in Database:${NC} ${GREEN}$total_reservations${NC}\n"

# ============================================================
# SUMMARY
# ============================================================

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}✓ SEEDING COMPLETE${NC}"
echo -e "${BLUE}========================================${NC}\n"

echo -e "${CYAN}RESERVATIONS BY STATUS:${NC}"
echo -e "  PENDING:                 ${YELLOW}$pending_count${NC} (50%)"
echo -e "  CONFIRMED:               ${GREEN}$confirmed_count${NC} (35%)"
echo -e "  CANCELLED:               ${RED}$cancelled_count${NC} (15%)"
echo -e "  Failed/Skipped:          ${RED}$failed_count${NC}\n"

echo -e "${CYAN}TOTAL:${NC}"
echo -e "  Attempted Bookings:      ${BLUE}40${NC}"
echo -e "  Successful Bookings:     ${GREEN}$((pending_count + confirmed_count + cancelled_count))${NC}"
echo -e "  Total in Database:       ${GREEN}$total_reservations${NC}\n"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}USEFUL LINKS:${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "Kafka UI:       ${CYAN}http://localhost:8080${NC}"
echo -e "Swagger UI:     ${CYAN}http://localhost:8082/swagger-ui.html${NC}"
echo -e "Analytics:      ${CYAN}http://localhost:8084${NC}"