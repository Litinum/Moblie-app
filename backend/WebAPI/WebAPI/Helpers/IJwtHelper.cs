using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using WebAPI.Models;

namespace WebAPI.Helpers
{
    public interface IJwtHelper
    {
        string CreateToken(User model);
    }
}
