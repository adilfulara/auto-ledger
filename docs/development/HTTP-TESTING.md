# HTTP Request Files

The `backend/http/` directory contains IntelliJ HTTP Client request files for manual API testing.

## Prerequisites

1. Start the local development environment:
   ```bash
   make dev-start
   ```

2. Ensure the backend is running at `http://localhost:9090`

## Files

### `cars.http`

**Location:** `backend/http/cars.http`

19 HTTP requests covering the Cars API:
- List cars by user
- Get car by ID
- Get car statistics (MPG, fillup count)
- Create new cars (various units)
- Update car details
- Delete cars
- Error cases (validation, not found)

### `fillups.http`

**Location:** `backend/http/fillups.http`

23 HTTP requests covering the Fillups API:
- Get all fillups for a car
- Get recent fillups (with limit)
- Get fillup by ID
- Create fillups (normal, partial, missed)
- Update fillup details
- Delete fillups
- Error cases (validation, odometer decreasing, not found)

## How to Use

### In IntelliJ IDEA / WebStorm:

1. Open any `.http` file
2. Click the ▶️ icon next to a request
3. View the response in the tool window

### Variables

Both files use these variables (defined at the top):

```http
@baseUrl = http://localhost:9090
@aliceUserId = a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11
@bobUserId = b1ffcd88-8b1a-5df7-aa5c-5aa8ac271b22
@teslaCarId = c2ddef77-7a2b-6ef8-99fc-499b9d482c33
@civicCarId = d3eeff66-6b3c-7df9-88eb-388a8c593d44
@f150CarId = e4fff055-5c4d-8ef0-77da-277b7d604e55
```

These UUIDs correspond to the sample data loaded with the `local` Spring profile.

## Sample Requests

### List Alice's cars
```http
GET http://localhost:9090/api/cars?userId=a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11
```

### Get Tesla stats
```http
GET http://localhost:9090/api/cars/c2ddef77-7a2b-6ef8-99fc-499b9d482c33/stats
```

### Create a new fillup
```http
POST http://localhost:9090/api/fillups?carId=c2ddef77-7a2b-6ef8-99fc-499b9d482c33
Content-Type: application/json

{
  "date": "2024-06-01T10:00:00Z",
  "odometer": 10000,
  "fuelVolume": 12.5,
  "pricePerUnit": 4.50,
  "totalCost": 56.25,
  "isPartial": false,
  "isMissed": false
}
```

## Tips

- Run requests in order to see proper data flow
- Check the response status codes (200, 201, 400, 404)
- DELETE requests are commented out by default - uncomment to use
- Error case requests help verify validation logic
