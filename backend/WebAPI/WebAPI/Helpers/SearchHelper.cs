using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using WebAPI.Models;

namespace WebAPI.Helpers
{
    public class SearchHelper
    {

        public static IEnumerable<object> GetSearchDistance(IEnumerable<Advert> query, string[] searchTerms)
        {
            return query.Where(ad => searchTerms.Any(ad.Title.ToLower().Contains));
        }

        public static string[] RemoveNoise(string[] array)
        {
            string[] noiseArray = { "je", "se", "na", ",", ".", ";", ":", "!" };

            foreach (string noise in noiseArray)
            {
                array = array.Where((s, ind) => !s.Equals(noise)).ToArray();
            }

            return array;
        }
    }
}
