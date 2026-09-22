if redis.call('GET', KEYS[2]) ~= ARGV[1] then return 0 end
redis.call('DEL', KEYS[2])
local occupied = tonumber(redis.call('GET', KEYS[1]) or '0')
if occupied > 0 then redis.call('DECR', KEYS[1]) end
redis.call('ZREM', KEYS[3], ARGV[1])
return 1
