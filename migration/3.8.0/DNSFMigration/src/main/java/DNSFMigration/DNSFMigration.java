package DNSFMigration;

import javax.ws.rs.core.MediaType;
import com.sun.jersey.api.client.*;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.text.Normalizer;
import java.net.URL;
import java.lang.Integer;

public class DNSFMigration
{
  public static void main(String[] args)
  {
    if(args.length != 1 || args[0].equals(""))
    {
      System.err.println("Usage: java -jar DNSFMigration <Neo4j host or ip>");
      System.exit(-1);
    }

    String cypherUri = "";
    try
    {
      cypherUri = new URL(new URL("http://" + args[0] + ":7474"), "/db/data/cypher").toString();
    }
    catch(java.net.MalformedURLException e)
    {
      System.err.println("Malformed endpoint URL");
      System.exit(-2);
    }

    String match = DNSFMigration.cypherQuery(cypherUri, "MATCH (u:User) WHERE EXISTS(u.displayName) RETURN u.id AS id, u.displayName AS dn;");

    List<List<String>> records = parseMatchData(match);
    int rsize = records.size();
    Map<String, Object> params = new HashMap<String, Object>();
    for(int i = rsize; i-- > 0;)
    {
      List<String> user = records.get(i);

      Object id = user.get(0);
      Object dn = user.get(1);

      params.put("uid", id);
      params.put("dnsf", DNSFMigration.generateDisplayNamePermutations(dn.toString()));

      String r = DNSFMigration.cypherQuery(cypherUri, "MATCH (u:User {id: {uid} }) SET u.displayNameSearchField = {dnsf}", params);
    }
  }

  private static String cypherQuery(String cypherUri, String query)
  {
    return DNSFMigration.cypherQuery(cypherUri, query, new HashMap<String, Object>());
  }

  private static String cypherQuery(String cypherUri, String query, Map<String, Object> params)
  {
    StringBuilder pars = new StringBuilder();

    for(Map.Entry<String, Object> entry : params.entrySet())
      pars.append("\"" + entry.getKey() + "\":\"" + entry.getValue() + "\",");

    String paramsJson = "";
    if(params.isEmpty() == false)
    {
      paramsJson = pars.toString();
      paramsJson = paramsJson.substring(0, paramsJson.length() - 1);
    }

    String postData = "{\"query\":\"" + query + "\",\"params\":{" + paramsJson + "}}";

    ClientResponse response = Client.create()
      .resource(cypherUri)
      .accept(MediaType.APPLICATION_JSON_TYPE)
      .type(MediaType.APPLICATION_JSON_TYPE)
      .header("X-Stream", "true")
      .post(ClientResponse.class, postData);

    String res = response.getEntity(String.class);
    response.close();

    return res;
  }

  private static List<List<String>> parseMatchData(String requestResult)
  {
    String dataList = requestResult.substring(requestResult.indexOf("[[") + 1, requestResult.indexOf("]]") + 1);

    List<String> subarrays = DNSFMigration.split(dataList, "\\],\\[");

    List<List<String>> result = new ArrayList<List<String>>(subarrays.size());

    for(int i = subarrays.size(); i-- > 0;)
    {
      List<String> userData = DNSFMigration.split(subarrays.get(i), "\",\"");
      if(i == 0)
        userData.set(0, userData.get(0).substring(2));
      else
        userData.set(0, userData.get(0).substring(1));
      if(i == subarrays.size() - 1)
        userData.set(1, userData.get(1).substring(0, userData.get(1).length() - 2));
      else
        userData.set(1, userData.get(1).substring(0, userData.get(1).length() - 1));

      result.add(userData);
    }

    return result;
  }

	private static String generateDisplayNamePermutations(String displayName)
	{
		List<String> parts = DNSFMigration.split(removeAccents(displayName).toLowerCase().replaceAll("\\s+", " "), " ");
		StringBuilder permutations = new StringBuilder();

		if(parts.size() > 5) // Only compute the permutations for the first five terms, otherwise the string is too big
		{
			permutations.append(DNSFMigration.join(parts, ""));
			parts = parts.subList(0, 5);
		}
		permutations.append(DNSFMigration.createAllPermutations(parts));

		return permutations.toString();
  }

	private static String removeAccents(String str) {
		return Normalizer.normalize(str, Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}

  /**
   * Cutting a character string following a delimiter.
   *
   * @param regexDelim the regex delim
   * @param str the str
   *
   * @return the list<string>
   */
  private static List<String> split(String str, String regexDelim) {
      return Arrays.asList(str.split(regexDelim));
  }

  /**
   * Returns the concatenation of strings from a list using the
   * delimiter indicated.
   * Example: <code>join ([ "A", "B", "CD"], "/") -> "A / B / CD"</code>
   *
   * @param list the list of strings to concatenate.
   * @param delim the delimiter to be inserted between each value.
   *
   * @return the string containing the concatenated values.
   */
  private static String join(List<String> list, String delim) {
      boolean isDelimiter = false;
      final StringBuilder builder = new StringBuilder();
      if (list != null) {
          for (String val : list) {
              if (isDelimiter) {
                  builder.append(delim);
              }
              builder.append(val);
              isDelimiter = true;
          }
      }
      return builder.toString();
  }

    /**
     * Generates a string containing all permutations of the given string list (maximum 16 strings)
     * Example: <code>createAllPermutations ([ "A", "B", "C"]) -> "ABCACBACAB"</code>
     *
     * @param list the list of strings to permute.
     *
     * @return the string containing all permutations.
     */
    private static String createAllPermutations(List<String> list)
    {
        int size = list.size();
        if(size > 16) // The limit is 16 because 16*4 == 64 == number of bits in a long. 20! > MAX_LONG so that would be the hard limit anyway
            throw new IllegalArgumentException("The number of string to permute exceeds 16");
        else if(size == 0)
            return "";
        else if(size == 1)
            return list.get(0);

        int nbPermuts = size; // the number of permutations is size!
        for(int i = size; i-- > 1;)
            nbPermuts *= i;

        StringBuilder str = new StringBuilder();
        List<Long> permuts = new ArrayList<Long>(nbPermuts);
        Map<Long, List<Integer>> prefixIndex = new HashMap<Long, List<Integer>>();
        Set<Integer> usedPermuts = new HashSet<Integer>();

        // Recursively generate all permutations
        DNSFMigration.generateAllPermutations(permuts, prefixIndex, size, 0, 0);

        Long current = permuts.get(0);
        usedPermuts.add(new Integer(0));

        int elementsToRead = size;
        for(int i = nbPermuts; i-- > 0;)
        {
            long ptr = 0xF << ((elementsToRead - 1) * 4);
            long val = current.longValue();

            // Only append the necessary strings
            for(int e = elementsToRead; e-- > 0;)
            {
                int strToAppend = (int)((val & ptr) >>> (e * 4));
                ptr >>>= 4;
                str.append(list.get(strToAppend));
            }
            elementsToRead = 0;

            Long next = current;
            do
            {
                ++elementsToRead;
                // Find the best next permutation (aka current suffix matches the biggest next prefix possible) (e.g. aBCD should match BCDa)
                long prefixToClear = ((long)(size - elementsToRead));
                long prefixLengthToLookFor = prefixToClear << (15 * 4);
                val &= (~((long)0) ^ (0xF << (prefixToClear * 4)));

                List<Integer> nextCandidates = prefixIndex.get(new Long(prefixLengthToLookFor | val));

                if(nextCandidates == null)
                {
                    System.err.println("\n\nImpossible error in createAllPermutations: No candidates\n\n");
                    continue;
                }
                else
                {
                    // Find a permutation that hasn't already been used
                    for(int candidateIx = nextCandidates.size(); candidateIx-- > 0;)
                    {
                        Integer candidate = nextCandidates.get(candidateIx);
                        if(usedPermuts.contains(candidate) == false)
                        {
                            usedPermuts.add(candidate);
                            next = permuts.get(candidate.intValue());
                            break;
                        }
                    }
                }
            }
            // If we can't find a n-sized prefix, try a (n-1)-sized prefix on the next iteration
            while(next == current && val != 0 && usedPermuts.size() != nbPermuts);

            current = next;
        }

        if(usedPermuts.size() != nbPermuts)
          System.err.println("\n\nImpossible error in createAllPermutations: Used " + usedPermuts.size() + " out of " + nbPermuts + "\n\n");

        return str.toString();
    }

    // nbElements must be at most 16. Call with permut = 0, taken = 0
    private static void generateAllPermutations(List<Long> allPermuts, Map<Long, List<Integer>> prefixIndex, int nbElements,
        long permut, long taken)
    {
        boolean isFinished = true;
        for(int i = nbElements; i-- > 0;)
        {
            if((taken & (1 << i)) != 0)
                continue;
            else
            {
                isFinished = false;
                generateAllPermutations(allPermuts, prefixIndex, nbElements, (permut << 4) | i, taken | (1 << i));
            }
        }

        if(isFinished == true)
        {
            Long finalPermut = new Long(permut);
            Integer permutIx = new Integer(allPermuts.size());
            allPermuts.add(finalPermut);

            for(long i = nbElements; i-- > 1;)
            {
                permut >>>= 4;

                // The prefixed length makes it possible to distinguish 0x01 and 0x1
                long prefixLength = (i << (15 * 4));
                Long prefix = new Long(prefixLength | permut);

                List<Integer> prefixes = prefixIndex.get(prefix);
                if(prefixes == null)
                {
                    prefixes = new ArrayList<Integer>(nbElements); // Real capacity should be (nbElements - 1)! but this is good enough
                    prefixIndex.put(prefix, prefixes);
                }
                prefixes.add(permutIx);
            }
        }
    }
}
