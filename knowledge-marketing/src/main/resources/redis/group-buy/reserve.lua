local current = redis.call('GET', KEYS[2])
if current then
  if current == ARGV[1] then return 2 else return -2 end
end
if redis.call('EXISTS', KEYS[1]) == 0 then
  redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[5], 'NX')
end
-- Keep the evidence longer than the business timeout so a delayed compensator can still identify the owner.
redis.call('SET', KEYS[2], ARGV[1], 'EX', ARGV[5], 'NX')
local occupied = redis.call('INCR', KEYS[1])
redis.call('EXPIRE', KEYS[1], ARGV[5])
if occupied > tonumber(ARGV[3]) then
  redis.call('DECR', KEYS[1])
  redis.call('DEL', KEYS[2])
  return 0
end
redis.call('ZADD', KEYS[3], ARGV[6], ARGV[1])
redis.call('EXPIRE', KEYS[3], ARGV[5])
return 1
