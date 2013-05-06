print("This is the demo!")

function SmartSilent_enter(name)
	lib:print("Welcome " .. name)
    if name == "Home" then
        lib:print("Go silent!")
        lib:setRingVolume(-1)
    end
end

function SmartSilent_leave(name)
	lib:print("Goodbye " .. name)
end
