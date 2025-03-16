select id, name, email, password, address 
from users 
where id >= 1 AND name == "name1" OR name == "name2" AND NOT address == "address1"
order_by id
offset 5
limit 10